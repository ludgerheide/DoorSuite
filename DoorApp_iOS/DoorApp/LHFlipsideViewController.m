//
//  LHFlipsideViewController.m
//  DoorApp
//
//  Created by Ludger Heide on 01.12.12.
//  Copyright (c) 2012 Ludger Heide. All rights reserved.
//

#import "LHFlipsideViewController.h"

@interface LHFlipsideViewController ()

@end

@implementation LHFlipsideViewController

@synthesize tfAddress;
@synthesize swAuthOnGPS;
@synthesize swAuthOnWLAN;

- (void)awakeFromNib
{
    self.contentSizeForViewInPopover = CGSizeMake(320.0, 480.0);
    [super awakeFromNib];
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    [self.delegate flipsideViewControllerDidLoad: self];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Actions

- (IBAction)done:(id)sender
{
    [self.delegate flipsideViewControllerDidFinish:self];
}

- (void)viewDidUnload {
    [self setTfAddress:nil];
    [self setSwAuthOnWLAN:nil];
    [self setSwAuthOnGPS:nil];
    [super viewDidUnload];
}
@end
