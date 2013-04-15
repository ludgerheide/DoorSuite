#include <SoftwareSerial.h>
#include <Timer.h>
#include <Event.h>

#define IN_FRONTBELL 2 //Must be 2 or 3 for interrupts

//Serial Pins
#define SOFTWARESERIALTX 5
#define SOFTWARESERIALRX 6
//Flat door control
#define OUT_FLATBUZZER 10

//Front door control
#define OUT_FRONTBUZZER 11 //Operates the door buzzer
#define OUT_GABELPIN 12 //Simulates hanging up the phone to reset bell State
#define OUT_SILENTPIN 13 //Ringer/Silent switch

//TImeout/Cooldown values
#define AUTHTIMEOUT 120L //The timout value in seconds
#define GABELTIMEOUT 500 //Bell reset length in milliseconds
#define FRONTBUZZERTIMEOUT 500 //Buzzer reset length in milliseconds
#define FLATBUZZERTIMEOUT 1000 //Flat Buzzer time in milliseconds
#define FRONTBUZZERCOOLDOWN 3000 //Do not allow triggering for this period
#define SERIALCOOLDOWN 1000 //How long we ignore serial after we get a byte

#define GROUND_ON HIGH //High for darlington array, low for crazy direct method
#define GROUND_OFF LOW
#define GEDENKSEKUNDE 200 //The chip needs that to work

int authTimer = -1;
int gabelTimer = -1;
int frontBuzzerTimer = -1;
int flatBuzzerTimer = -1;
int cooldownTimer = -1;
int serialCooldown = -1;

int openingsRemaining = 2;

boolean authenticated = false;
boolean frontBuzzerOnCooldown = false;
boolean serialOnCooldown = false;
volatile boolean frontBellRinging = false;
volatile boolean flatBellRinging = false;

int inByte = 0;
Timer t_auth;
Timer t_gabel;
Timer t_frontBuzzer;
Timer t_flatBuzzer;
Timer t_cooldown;
Timer t_serialCooldown;

//Stuff for FlatDoor
char authCodeBuffer[4];
char receivedCodeBuffer[4];

//SoftwareSerial
SoftwareSerial displaySerial =  SoftwareSerial(SOFTWARESERIALRX, SOFTWARESERIALTX);

void setup() {
  pinMode(IN_FRONTBELL, INPUT);
  digitalWrite(IN_FRONTBELL, LOW); //DONe: Change Back for prod
  attachInterrupt(IN_FRONTBELL - 2, frontBell, HIGH); //DONE: Change back for prod

  pinMode(OUT_FLATBUZZER, OUTPUT);
  digitalWrite(OUT_FLATBUZZER, GROUND_OFF);

  pinMode(OUT_FRONTBUZZER, OUTPUT);
  digitalWrite(OUT_FRONTBUZZER, GROUND_OFF);

  pinMode(OUT_GABELPIN, OUTPUT);
  digitalWrite(OUT_GABELPIN, GROUND_OFF);

  pinMode(OUT_SILENTPIN, OUTPUT);
  digitalWrite(OUT_SILENTPIN, GROUND_OFF);
  
  pinMode(SOFTWARESERIALRX, INPUT);
  pinMode(SOFTWARESERIALTX, OUTPUT);
  
  // start serial port at 9600 bps and wait for port to open:
  Serial.begin(9600);
  
  //STart a SoftwareSerial
  displaySerial.begin(9600);
  
}

void authenticate() {
  //Stop any timers from earlier authentications
  if(authTimer != -1)
    t_auth.stop(authTimer);
  authTimer = t_auth.after(AUTHTIMEOUT * 1000L, deauthenticate);
  authenticated = true;

  //Generate a random code and display it
  for(byte i = 0; i < 4; i++) {
    authCodeBuffer[i] = random('0','9'); //DONE: change for PROD
    displaySerial.print(authCodeBuffer[i]);  //DONE:Change to SoftwareSerial 
  }    
  //Serial.println(' '); //DONE: Remove

  digitalWrite(OUT_SILENTPIN, GROUND_ON);
  delay(GEDENKSEKUNDE);
  Serial.println("Authenticated");
}  

void deauthenticate() {
  authenticated = false;
  t_auth.stop(authTimer);
  openingsRemaining = 2;
  authTimer = -1;

  //remove the code from the display
  displaySerial.print('v'); //DONE:change to print(0x76);
  digitalWrite(OUT_SILENTPIN, GROUND_OFF);
  delay(GEDENKSEKUNDE);
  Serial.println("Deauthenticated");
}

void frontBell() {
  frontBellRinging = true;
}

void flatBell() {
  flatBellRinging = true;
}

void gabelOn() {
  if(gabelTimer != -1)
    t_gabel.stop(gabelTimer); 
  digitalWrite(OUT_GABELPIN, GROUND_ON);
  gabelTimer = t_gabel.after(GABELTIMEOUT, gabelOff);
  delay(GEDENKSEKUNDE);
  Serial.println("GabelOn");
}

void gabelOff() {
  t_gabel.stop(gabelTimer);
  gabelTimer = -1;
  digitalWrite(OUT_GABELPIN, GROUND_OFF);
  delay(GEDENKSEKUNDE);
  Serial.println("GabelOff");
}

void frontBuzzerOn() {
  Serial.print("FrontBuzzerOn called - ");
  if(frontBuzzerOnCooldown == false) {
    if(frontBuzzerTimer != -1)
      t_frontBuzzer.stop(frontBuzzerTimer);
    frontBuzzerTimer = t_frontBuzzer.after(FRONTBUZZERTIMEOUT, frontBuzzerOff);
    openingsRemaining -= 1;

    //Set cooldown on and start the timer
    cooldownTimer = t_cooldown.after(FRONTBUZZERCOOLDOWN, enableFrontBuzzer);
    frontBuzzerOnCooldown = true;

    digitalWrite(OUT_FRONTBUZZER, GROUND_ON);
    delay(GEDENKSEKUNDE);
    Serial.println("opening");
  }
  Serial.println("on cooldown, not opening");
}

void frontBuzzerOff() {
  t_frontBuzzer.stop(frontBuzzerTimer);
  frontBuzzerTimer = -1;
  digitalWrite(OUT_FRONTBUZZER, GROUND_OFF);
  Serial.println("FrontBuzzerOff");
} 

void enableFrontBuzzer() {
  t_cooldown.stop(cooldownTimer);
  cooldownTimer = -1;
  frontBuzzerOnCooldown = false;
  Serial.println("Cooldown off");
}

void flatBuzzerOn() {
  if(authenticated == true) {
    byte codeIsCorrect = true;
    for(byte i = 0; i < 4; i++) {
      if(authCodeBuffer[i] != receivedCodeBuffer[i]) {
        codeIsCorrect = false;        
      }
    }
    if(codeIsCorrect) {
      Serial.println("201 FlatDoor Correct!");
      if(flatBuzzerTimer != -1)
        t_flatBuzzer.stop(flatBuzzerTimer);
      flatBuzzerTimer = t_flatBuzzer.after(FLATBUZZERTIMEOUT, flatBuzzerOff);
      digitalWrite(OUT_FLATBUZZER, GROUND_ON);
      deauthenticate();
    } 
    else {
      Serial.println("400 Code Incorrect!");
    }
  } 
  else {
    Serial.println("401 Not authenticated!");
  }
}

void flatBuzzerOff() {
  t_flatBuzzer.stop(flatBuzzerTimer);
  flatBuzzerTimer = -1;
  digitalWrite(OUT_FLATBUZZER, GROUND_OFF);
  Serial.println("FlatBuzzerOff");
} 

void enableSerial() {
  t_serialCooldown.stop(serialCooldown);
  serialCooldown = -1;
  serialOnCooldown = false;
}


void loop() {
  t_auth.update();
  t_gabel.update();
  t_frontBuzzer.update();
  t_flatBuzzer.update();
  t_cooldown.update();
  t_serialCooldown.update();
  // if we get a valid string, authenticate
  if (Serial.available()) {
    // get incoming byte:
    inByte = Serial.read();
    //We need to ignore bytes if we are on cooldown
    if(!serialOnCooldown) {
      if(inByte == 97) {
        Serial.println("200 AuthOn serial");
        if(authenticated)
          deauthenticate();
        authenticate();
        //Set cooldown on and start the timer
        serialCooldown = t_serialCooldown.after(SERIALCOOLDOWN, enableSerial);
        serialOnCooldown = true;
      }
      if(inByte == 98) {
        Serial.readBytesUntil(0x76, receivedCodeBuffer, 4);
        flatBuzzerOn();
        //Set cooldown on and start the timer
        serialCooldown = t_serialCooldown.after(SERIALCOOLDOWN, enableSerial);
        serialOnCooldown = true;
      }
    }
  }

  //If the bell has been tung, we do stuff, the reset its state
  if(frontBellRinging == true) {
    Serial.println("FrontBell Ringing");
    if(authenticated == true && openingsRemaining > 0) {
      gabelOn();
      delay(GEDENKSEKUNDE);
      frontBuzzerOn();
    }
    frontBellRinging = false;
  }
}











