//
//  LHMainViewController.m
//  DoorApp
//
//  Created by Ludger Heide on 01.12.12.
//  Copyright (c) 2012 Ludger Heide. All rights reserved.
//

#import "LHMainViewController.h"

@interface LHMainViewController ()

@end

@implementation LHMainViewController

@synthesize connection;

- (void)viewDidLoad
{
    [super viewDidLoad];
    //Load the preferences
    url = [[NSUserDefaults standardUserDefaults] objectForKey: URLKey];
    authOnGPS = [[[NSUserDefaults standardUserDefaults] objectForKey: AuthOnGPSKey] boolValue];
    authOnWLAN = [[[NSUserDefaults standardUserDefaults] objectForKey: AuthOnWLANKey] boolValue];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Flipside View Controller

- (void)flipsideViewControllerDidLoad:(LHFlipsideViewController *)controller
{
    controller.tfAddress.text = url;
    controller.swAuthOnGPS.on = authOnGPS;
    controller.swAuthOnWLAN.on = authOnWLAN;
}

- (void)flipsideViewControllerDidFinish:(LHFlipsideViewController *)controller
{
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone) {
        [self dismissViewControllerAnimated:YES completion:nil];
    } else {
        [self.flipsidePopoverController dismissPopoverAnimated:YES];
        self.flipsidePopoverController = nil;
    }
    //Get the changed prefs back into the main class
    url = controller.tfAddress.text;
    authOnGPS = controller.swAuthOnWLAN.on;
    authOnWLAN = controller.swAuthOnGPS.on;
    
    //Write them to the settings
    [[NSUserDefaults standardUserDefaults] setValue: url forKey: URLKey];
    [[NSUserDefaults standardUserDefaults] setValue: [NSNumber numberWithBool: authOnGPS] forKey: AuthOnGPSKey];
    [[NSUserDefaults standardUserDefaults] setValue: [NSNumber numberWithBool: authOnWLAN] forKey: AuthOnWLANKey];
    
}

- (void)popoverControllerDidDismissPopover:(UIPopoverController *)popoverController
{
    self.flipsidePopoverController = nil;
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    if ([[segue identifier] isEqualToString:@"showAlternate"]) {
        [[segue destinationViewController] setDelegate:self];
        
        if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPad) {
            UIPopoverController *popoverController = [(UIStoryboardPopoverSegue *)segue popoverController];
            self.flipsidePopoverController = popoverController;
        }
    }
}

- (IBAction)togglePopover:(id)sender
{
    if (self.flipsidePopoverController) {
        [self.flipsidePopoverController dismissPopoverAnimated:YES];
        self.flipsidePopoverController = nil;
    } else {
        [self performSegueWithIdentifier:@"showAlternate" sender:sender];
    }
}

#pragma mark - UI-triggered methods

- (IBAction)process:(id)sender {    
    
    //Creates the URL request. Don't use cache
    NSMutableURLRequest *theRequest = [NSMutableURLRequest requestWithURL:[NSURL URLWithString: url] cachePolicy: NSURLRequestReloadIgnoringLocalAndRemoteCacheData timeoutInterval:60];
    
    //Search the keychain for our secret, add a random one if none exists.
    NSString *secret = [KeychainWrapper keychainStringFromMatchingIdentifier: LHDoorAppSecret];
    if(secret == nil) {
        //Create a random string
        secret = [self generateRandomString: 256];
        [KeychainWrapper createKeychainValue: secret forIdentifier: LHDoorAppSecret];
    }
    
    //Set the POST that includes our auth request
    theRequest.HTTPMethod = @"POST";
    theRequest.HTTPBody = [[NSString stringWithFormat: @"deviceName=%@&udid=%@&secret=%@", [[UIDevice currentDevice] name], [[[UIDevice currentDevice] identifierForVendor] UUIDString], secret] dataUsingEncoding:NSUTF8StringEncoding];
    theRequest.TimeoutInterval = 5.0;
    
	//Creates the URL connection with the request and starts loading thedata.
    
    connection = [[NSURLConnection alloc] initWithRequest:theRequest delegate:self startImmediately:YES];
    
    //Initialize the Data
    receivedData = [[NSMutableData alloc] init];
    
    //Start the acitivity indicator
    [aiActivity startAnimating];
    laAuthState.text = @"Status: Connecting.";
    laAuthState.textColor = [UIColor yellowColor];
    buProcess.enabled = NO;
    if(authCountdown)
        [authCountdown invalidate];
}

-(NSString *)generateRandomString: (int)length {
    NSMutableString *randomString = [NSMutableString stringWithCapacity: length];
    for (int i=0; i < length; i++) {
        [randomString appendFormat:@"%c", (char)(65 + (arc4random() % 25))];
    }
    return randomString;
}

-(void)advanceTimer: (NSTimer *)timer {
    time -= 1;
    if(time > 0)
        laAuthState.text = [NSString stringWithFormat: @"Status: Authenticated – %hd", time];
    else
        [self resetUI];
}

/*
 ------------------------------------------------------------------------
 NSURLConnection delegate methods for asynchronous requests
 ------------------------------------------------------------------------
 */

- (void) connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response
{
    /* Called when the server has determined that it has enough
	 information to create the NSURLResponse. It can be called
	 multiple times (for example in the case of a redirect), so
	 each time we reset the data. */
    [receivedData setLength:0];
    laAuthState.text = [laAuthState.text stringByAppendingString: @"."];
    NSLog(@"DidReceiveResponse!");
}

- (void) connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
    /* appends the new data to the received data */
    [receivedData appendData:data];
    laAuthState.text = [laAuthState.text stringByAppendingString: @"."];
    NSLog(@"DidReceiveData!");
}

- (void) connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{    
	NSString *errorString = [NSString stringWithFormat: @"The server reports: %@\n%@", [error localizedDescription], [[error userInfo] objectForKey: NSURLErrorFailingURLStringErrorKey]];
    NSLog(errorString);
    UIAlertView *connectionAlert = [[UIAlertView alloc] initWithTitle: @"Connection failed!" message: errorString delegate: nil cancelButtonTitle: @"OK" otherButtonTitles: nil];
    [connectionAlert show];
    
    [self resetUI];
}

- (void) connectionDidFinishLoading:(NSURLConnection *)connection {    
    NSLog(@"DidFinishLoading!");
    NSString *serverReturned = [[NSString alloc] initWithData: receivedData encoding: NSUTF8StringEncoding];
    NSLog(serverReturned);
    
    if([serverReturned isEqualToString: ServerReturnSuccess]) {
        //Start a timer counting doen the time we are still authenticated
        time = AUTHTIMEOUT;
        authCountdown = [NSTimer timerWithTimeInterval: 1 target: self selector: @selector(advanceTimer:) userInfo:nil repeats:YES];
        [[NSRunLoop currentRunLoop] addTimer: authCountdown forMode: NSDefaultRunLoopMode];
        
        //Reset the UI
        laAuthState.text = [NSString stringWithFormat: @"Status: Authenticated – %hd", time];
        laAuthState.textColor = [UIColor greenColor];
        [aiActivity stopAnimating];
        
        /* releases the connection */
        if (self.connection) {
            self.connection = nil;
        }
        
    } else {
        NSString *reason;
        if([serverReturned isEqualToString: ServerReturnAuthFailure])
            reason = @"The server reported an authentication failure. Please contact the administrator";
        else if([serverReturned isEqualToString: ServerReturnInternalFailure])
            reason = @"The server reported an internal failure. Please contact the administrator";
        else if([serverReturned isEqualToString: ServerReturnRegistrationStarted])
            reason = @"Your device has been registered and is pending confirmation. If this is not the first time you see this alert, please contact the administrator";
        else if([serverReturned isEqualToString: ServerReturnRegistrationPending])
            reason = @"Your device is still pending confirmation. Please contact the administrator";
        else
            reason = @"Unknown failure. Please contact the administrator";
        
        //Create an alert and show it
        UIAlertView *failureAlert = [[UIAlertView alloc] initWithTitle: @"Authentication failed!" message: reason delegate: nil cancelButtonTitle: @"OK" otherButtonTitles: nil];
        [failureAlert show];
        
        [self resetUI];
    }
}

-(void)resetUI{
    //Reset the UI
    laAuthState.text = @"Status: Not authenticated";
    laAuthState.textColor = [UIColor redColor];
    [aiActivity stopAnimating];
    buProcess.enabled = YES;
	/* releases the connection */
	if (self.connection) {
		self.connection = nil;
    }
}

//Authentication Handling
- (BOOL)connection:(NSURLConnection *)connection canAuthenticateAgainstProtectionSpace:(NSURLProtectionSpace *)protectionSpace {
    return [protectionSpace.authenticationMethod isEqualToString:NSURLAuthenticationMethodServerTrust];
}

- (void)connection:(NSURLConnection *)connection didReceiveAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge
{
    
    if ([challenge previousFailureCount] > 0) {
        [[challenge sender] cancelAuthenticationChallenge:challenge];
        NSLog(@"Bad Username Or Password");
        return;
    }
    
    if ([challenge.protectionSpace.authenticationMethod isEqualToString:NSURLAuthenticationMethodServerTrust]) {        
        SecTrustResultType result;
        //This takes the serverTrust object and checkes it against the stored cert
        NSString *          path;
        NSData *            data;
        SecCertificateRef   cert;
        
        path = [[NSBundle mainBundle] pathForResource:@"LHRoot" ofType:@"der"];
        assert(path != nil);
        
        data = [NSData dataWithContentsOfFile:path];
        assert(data != nil);
        
        cert = SecCertificateCreateWithData(NULL, (__bridge CFDataRef) data);
        assert(cert != NULL);
        
        SecCertificateRef* certArray = {&cert};
        
        CFArrayRef LHCert = CFArrayCreate (NULL, certArray, 1, NULL);
        
        SecTrustSetAnchorCertificates(challenge.protectionSpace.serverTrust, LHCert);
        SecTrustEvaluate(challenge.protectionSpace.serverTrust, &result);
        
        
        //When testing this against a trusted server I got kSecTrustResultUnspecified every time. But the other two match the description of a trusted server
        if(result == kSecTrustResultProceed || result == kSecTrustResultConfirm ||  result == kSecTrustResultUnspecified){
            [challenge.sender useCredential:
             [NSURLCredential credentialForTrust: challenge.protectionSpace.serverTrust]
                 forAuthenticationChallenge: challenge];
        } else {
            UIAlertView *certAlert = [[UIAlertView alloc] initWithTitle: @"SSL failed!" message: @"The server did not provide a valide certificate for authentication!" delegate: nil cancelButtonTitle: @"OK" otherButtonTitles: nil];
            [certAlert show];
            [self resetUI];
        }
    }
}

- (void)viewDidUnload {
    buProcess = nil;
    laAuthState = nil;
    aiActivity = nil;
    [super viewDidUnload];
}
@end
