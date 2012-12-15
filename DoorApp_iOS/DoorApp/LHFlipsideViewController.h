//
//  LHFlipsideViewController.h
//  DoorApp
//
//  Created by Ludger Heide on 01.12.12.
//  Copyright (c) 2012 Ludger Heide. All rights reserved.
//

#import <UIKit/UIKit.h>

@class LHFlipsideViewController;

@protocol LHFlipsideViewControllerDelegate
- (void)flipsideViewControllerDidFinish:(LHFlipsideViewController *)controller;
- (void)flipsideViewControllerDidLoad:(LHFlipsideViewController *)controller;
@end

@interface LHFlipsideViewController : UIViewController

@property (weak, nonatomic) IBOutlet UITextField *tfAddress;
@property (weak, nonatomic) IBOutlet UISwitch *swAuthOnWLAN;
@property (weak, nonatomic) IBOutlet UISwitch *swAuthOnGPS;
@property (weak, nonatomic) id <LHFlipsideViewControllerDelegate> delegate;

- (IBAction)done:(id)sender;

@end
