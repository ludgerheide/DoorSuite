#include <Timer.h>
#include <Event.h>

#define IN_BELLPIN 2 //Must be 2 or 3 for interrupts
#define IN_AUTHPIN 3 //Must be 2 or 3 for interrupts

#define OUT_BUZZERPIN 11 //Operates the door buzzer
#define OUT_GABELPIN 12 //Simulates hanging up the phone to reset bell State
#define OUT_SILENTPIN 13 //Ringer/Silent switch

#define AUTHTIMEOUT 30 //The timout value in seconds
#define GABELTIMEOUT 500 //Bell reset length in milliseconds
#define BUZZERTIMEOUT 500 //Buzzer reset length in milliseconds
#define BUZZERCOOLDOWN 6000 //Do not allow triggering for this period

#define GROUND_ON HIGH //High for darlington array, low for crazy direct method
#define GROUND_OFF LOW
#define GEDENKSEKUNDE 200 //The chip needs that to work

int authTimer = -1;
int gabelTimer = -1;
int buzzerTimer = -1;
int cooldownTimer = -1;

int openingsRemaining = 2;

boolean authenticated = false;
boolean buzzerOnCooldown = false;
volatile boolean isRinging = false;
volatile boolean authButtonPressed = false;

int inByte = 0;
Timer t_auth;
Timer t_gabel;
Timer t_buzzer;
Timer t_cooldown;

void setup() {
  // start serial port at 9600 bps and wait for port to open:
  Serial.begin(9600);

  pinMode(IN_BELLPIN, INPUT);
  digitalWrite(IN_BELLPIN, LOW);
  attachInterrupt(IN_BELLPIN - 2, bell, RISING);

  pinMode(IN_AUTHPIN, INPUT);
  digitalWrite(IN_AUTHPIN, HIGH);
  attachInterrupt(IN_AUTHPIN - 2, authButton, LOW);

  pinMode(OUT_BUZZERPIN, OUTPUT);
  digitalWrite(OUT_BUZZERPIN, GROUND_OFF);
  
  pinMode(OUT_GABELPIN, OUTPUT);
  digitalWrite(OUT_GABELPIN, GROUND_OFF);
  
  pinMode(OUT_SILENTPIN, OUTPUT);
  digitalWrite(OUT_SILENTPIN, GROUND_OFF);
}

void authenticate() {
  //Stop any timers from earlier authentications
  if(authTimer != -1)
    t_auth.stop(authTimer);
  authTimer = t_auth.after(AUTHTIMEOUT * 1000, deauthenticate);
  authenticated = true;
  digitalWrite(OUT_SILENTPIN, GROUND_ON);
  delay(GEDENKSEKUNDE);
  Serial.println("Authenticated");
}  

void deauthenticate() {
  authenticated = false;
  t_auth.stop(authTimer);
  openingsRemaining = 2;
  authTimer = -1;
  digitalWrite(OUT_SILENTPIN, GROUND_OFF);
  delay(GEDENKSEKUNDE);
  Serial.println("Deauthenticated");
}

void bell() {
  isRinging = true;
}

void authButton() {
  authButtonPressed = true;
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

void buzzerOn() {
  Serial.print("BuzzerOn called - ");
  if(buzzerOnCooldown == false) {
    if(buzzerTimer != -1)
      t_buzzer.stop(buzzerTimer);
    buzzerTimer = t_buzzer.after(BUZZERTIMEOUT, buzzerOff);
    openingsRemaining -= 1;
    
    //Set cooldown on and start the timer
    cooldownTimer = t_cooldown.after(BUZZERCOOLDOWN, enableBuzzer);
    buzzerOnCooldown = true;
    
    digitalWrite(OUT_BUZZERPIN, GROUND_ON);
    delay(GEDENKSEKUNDE);
    Serial.println("opening");
  }
  Serial.println("on cooldown, not authenticating");
}

void buzzerOff() {
  t_buzzer.stop(buzzerTimer);
  buzzerTimer = -1;
  digitalWrite(OUT_BUZZERPIN, GROUND_OFF);
  Serial.println("BuzzerOff");
} 

void enableBuzzer() {
  t_cooldown.stop(cooldownTimer);
  cooldownTimer = -1;
  buzzerOnCooldown = false;
  Serial.println("Cooldown off");
}

void loop() {
  t_auth.update();
  t_gabel.update();
  t_buzzer.update();
  t_cooldown.update();
  // if we get a valid byte, authenticate
  if (Serial.available()) {
    // get incoming byte:
    inByte = Serial.read();
    if(inByte == 97) {
      Serial.println("AuthOn serial");
      deauthenticate;
      authenticate();
    }
  }

  //If the authButton has been pressed, we do stuff, then reset its state
  if(authButtonPressed == true) {
    Serial.println("authButton pressed");
    deauthenticate();
    authenticate();
    isRinging = true;
    authButtonPressed = false;
  }
  
  //If the bell has been tung, we do stuff, the reset its state
  if(isRinging == true) {
    Serial.println("isRinging");
    detachInterrupt(IN_BELLPIN - 2);
    if(authenticated == true && openingsRemaining > 0) {
      gabelOn();
      delay(GEDENKSEKUNDE);
      buzzerOn();
    }
    if(openingsRemaining <= 0) {
      deauthenticate;
    }
    attachInterrupt(IN_BELLPIN - 2, bell, RISING);
    isRinging = false;
  }
}



