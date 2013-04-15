//
//  LHMainViewController.h
//  DoorApp
//
//  Created by Ludger Heide on 01.12.12.
//  Copyright (c) 2012 Ludger Heide. All rights reserved.
//

#import "LHFlipsideViewController.h"
#import "KeychainWrapper.h"

@interface LHMainViewController : UIViewController <LHFlipsideViewControllerDelegate, UIPopoverControllerDelegate, NSURLConnectionDelegate, UITextFieldDelegate>
{
    NSURLConnection *connection;
    NSMutableData *receivedData;
    NSString* urlString;
    NSTimer *authCountdown;
    
    short time;
    BOOL authOnGPS;
    BOOL authOnWLAN;
    
    __weak IBOutlet UIButton *buFrontDoor;
    __weak IBOutlet UILabel *laAuthState;
    __weak IBOutlet UIActivityIndicatorView *aiActivity;
    __weak IBOutlet UITextField *tfAuthCode;
    __weak IBOutlet UIButton *buAbort;
}

@property (strong, nonatomic) UIPopoverController *flipsidePopoverController;
@property (strong, nonatomic) NSURLConnection *connection;

-(BOOL)stringIsNumeric:(NSString*) string;
-(IBAction)process:(id)sender;
-(IBAction)abort:(id)sender;
-(NSString *)generateRandomString: (int) length;
-(void)advanceTimer: (NSTimer *)timer;
-(void)resetUI;

@end