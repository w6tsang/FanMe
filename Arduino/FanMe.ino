/*
  NOTES:
  -consider PWM usage for feedback control on potentionmeter inputs tracking rotation of gears for limits
*/
bool InitSystem = true;
String Direction_Cmd = "none";
bool ManualMode = false;

double curSector = 2;
double nextSector = 0;
double baseSteps = 3000;

String serialModeInput = "none";

//Globals for Protothreading*
#include "Metro.h"
Metro TempSensorRead_Metro = Metro(3000);
Metro ArraySensorWarmup_Metro = Metro(30000);

//Globals for Bluetooth*
#include "SoftwareSerial.h"
SoftwareSerial Bluetooth(10,11); // RX | TX -> need PWM pins

#include "cactus_io_DHT22.h"
#define DHT22_PIN 2 // what pin on the arduino is the DHT22 data line connected to
DHT22 tempSensor(DHT22_PIN);

//Globals for Infrared Sensor Array (1 to 9)*
int ArraySensor1 = 3;
int ArraySensor2 = 4;
int ArraySensor3 = 5;
int ArraySensor4 = 6;
int ArraySensor5 = 7;
int ArraySensor6 = 8;
int ArraySensor7 = 9;
int ArraySensor8 = 10;
int ArraySensor9 = 11;

//Globals for Fanspeed Control (0,#,#,# rpm)*
int Speed_off = 22;
int Speed_slow = 23;
int Speed_medium = 24;
int Speed_fast = 25;

//Globals for Stepper Motor Control (a,b,c,d - windings)*
int Motor1_a = 28;
int Motor1_b = 29;
int Motor1_c = 30;
int Motor1_d = 31;
int Motor2_a = 32;
int Motor2_b = 33;
int Motor2_c = 34;
int Motor2_d = 35;
int Motor3_a = 36; // might not need 3rd motor...used for pumping hot air
int Motor3_b = 37;
int Motor3_c = 38;
int Motor3_d = 39;
int Motor1_step = 0;
int Motor2_step = 0;
int Motor3_step = 0;


//==================================================


void setup() {
  Serial.begin(9600);
  Bluetooth.begin(9600);
  tempSensor.begin();

  //Temperature Sensor*
  Serial.println("DHT22 Humidity - Temperature Sensor");
  Serial.println("RH\tTemp (C)\tTemp (F)\tHeat Index (C)\tHeat Index (F)");

  //Infrared Sensor Array*
  pinMode(ArraySensor1,INPUT);
  pinMode(ArraySensor2,INPUT);
  pinMode(ArraySensor3,INPUT);
  pinMode(ArraySensor4,INPUT);
  pinMode(ArraySensor5,INPUT);
  pinMode(ArraySensor6,INPUT);
  pinMode(ArraySensor7,INPUT);
  pinMode(ArraySensor8,INPUT);
  pinMode(ArraySensor9,INPUT);
  digitalWrite(ArraySensor1,LOW);
  digitalWrite(ArraySensor2,LOW);
  digitalWrite(ArraySensor3,LOW);
  digitalWrite(ArraySensor4,LOW);
  digitalWrite(ArraySensor5,LOW);
  digitalWrite(ArraySensor6,LOW);
  digitalWrite(ArraySensor7,LOW);
  digitalWrite(ArraySensor8,LOW);
  digitalWrite(ArraySensor9,LOW);

  //Fanspeed Relays*
  pinMode(Speed_off,OUTPUT);
  pinMode(Speed_slow,OUTPUT);
  pinMode(Speed_medium,OUTPUT);
  pinMode(Speed_fast,OUTPUT);
  digitalWrite(Speed_off,HIGH); // NO relay switch needs LOW to close switch
  digitalWrite(Speed_slow,HIGH);
  digitalWrite(Speed_medium,HIGH);
  digitalWrite(Speed_fast,HIGH);

  //Rotation and Tilt Stepper Motors*
  pinMode(Motor1_a, OUTPUT);
  pinMode(Motor1_b, OUTPUT);
  pinMode(Motor1_c, OUTPUT);
  pinMode(Motor1_d, OUTPUT);
}


//==================================================


void loop() {  
  CheckInitializeWarmupSystem();

  BluetoothConnection_Control();

  // The DHT22 should not be read at a frequency higher than 0.5 Hz, so add 3 second delay
  if (TempSensorRead_Metro.check())
    TemperatureSensor_Control();

  if (!InitSystem)
    InfraredSensorArray_Control();
    
  FanspeedRelay_Control();


//???????????????????????????????????????????????????????????
  // protoype: toggle auto and manual modes thru serial
  if (Serial.available() > 0)
  {
    serialModeInput = Serial.readString();
  }
  if (serialModeInput == "false")
    ManualMode = false;
  if (serialModeInput == "true")
    ManualMode = true;
  Serial.println("Mode=" + String(ManualMode));
//???????????????????????????????????????????????????????????


    
  int i = 0;
  if (!InitSystem){
    if(ManualMode){
      Serial.println("manual im in here");
      if (Direction_Cmd == "left" || Direction_Cmd == "right")
      {
        while (i < 1000){
          Motor1_step = RotationStepperMotor_Control(Motor1_step); 
          i++;
        }
        Direction_Cmd = "none";
      }
    }
    else{
      // Autonomous Mode      
      double deltaSec = 0;
      deltaSec = SemiAutoDetection();
      double steps = fabs(deltaSec)*baseSteps;
      Serial.print("Steps: ");
      Serial.println(steps);
      if (steps != 0){
        while (i < steps ) {
          Motor1_step = AutoRotationStepperMotor_Control(Motor1_step,deltaSec); 
          i++;
        }
      }
    }
  }

  //RotationStepperMotor_Control(Motor2_step);
  //RotationStepperMotor_Control(Motor3_step);
}


//==================================================


void CheckInitializeWarmupSystem(){
  // Wait for InfraredSensorArray to warm up: 1 minute
  // Wait for 'Function2... to warm up'
  if (InitSystem && ArraySensorWarmup_Metro.check()){
    InitSystem = false;
  }
}

void BluetoothConnection_Control(){
  // Keep reading from HC-05 and send to Arduino Serial Monitor
  if (Bluetooth.available()) {
    Direction_Cmd = Bluetooth.readString(); 
    if (Direction_Cmd == "automode"){
      ManualMode = false;
    }
    else if (Direction_Cmd == "manual"){
      ManualMode = true;
    }
    Serial.println(Direction_Cmd);
  }

  // Keep reading from Arduino Serial Monitor and send to HC-05
  if (Serial.available())
    Bluetooth.write("yeet");
}

void InfraredSensorArray_Control(){
  if (digitalRead(ArraySensor1)==HIGH) { Serial.println("INDEX1 - DETECTED"); }
    else { Serial.println("INDEX1 - STATIONARY"); }
  if (digitalRead(ArraySensor2)==HIGH) { Serial.println("INDEX2 - DETECTED"); }
    else { Serial.println("INDEX2 - STATIONARY"); }
  if (digitalRead(ArraySensor3)==HIGH) { Serial.println("INDEX3 - DETECTED"); }
    else { Serial.println("INDEX3 - STATIONARY"); }
  /*if (digitalRead(ArraySensor4)==HIGH) { Serial.println("INDEX4 - DETECTED"); }
    else { Serial.println("INDEX4 - STATIONARY"); }*/
}

void TemperatureSensor_Control(){
  // Reading temperature or humidity takes about 250 milliseconds!
  // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
  tempSensor.readHumidity();
  tempSensor.readTemperature();

  // Check if any reads failed and exit early (to try again).
  if (isnan(tempSensor.humidity) || isnan(tempSensor.temperature_C))
  {
    Serial.println("DHT sensor read failure!");
    return;
  }
  Serial.print(tempSensor.humidity); Serial.print(" %\t\t");
  Serial.print(tempSensor.temperature_C); Serial.print(" *C\t");
  Serial.print(tempSensor.temperature_F); Serial.print(" *F\t");
  Serial.print(tempSensor.computeHeatIndex_C()); Serial.print(" *C\t");
  Serial.print(tempSensor.computeHeatIndex_F()); Serial.println(" *F");
}

void FanspeedRelay_Control(){ 
  if (Direction_Cmd=="off" || Direction_Cmd=="slow" || Direction_Cmd=="medium" || Direction_Cmd=="fast")
  {
    if (Direction_Cmd=="off" || Direction_Cmd=="none") { digitalWrite(Speed_off,HIGH); }
    else{
      digitalWrite(Speed_off,LOW);
      digitalWrite(Speed_slow,LOW);
      digitalWrite(Speed_medium,LOW);
      digitalWrite(Speed_fast,LOW);
    }
    if (Direction_Cmd=="slow") { digitalWrite(Speed_slow,LOW); }
      else{ digitalWrite(Speed_slow,HIGH); }
    if (Direction_Cmd=="medium") { digitalWrite(Speed_medium,LOW); }
      else{ digitalWrite(Speed_medium,HIGH); }
    if (Direction_Cmd=="fast") { digitalWrite(Speed_fast,LOW); }
      else{ digitalWrite(Speed_fast,HIGH); }
    Serial.println(Direction_Cmd);
  } 
}

int RotationStepperMotor_Control(int motorStep){   
  switch(motorStep){   
    case 0:      
    digitalWrite(Motor1_a, LOW);
    digitalWrite(Motor1_b, LOW);
    digitalWrite(Motor1_c, LOW);
    digitalWrite(Motor1_d, HIGH);
    break;
    case 1:
    digitalWrite(Motor1_a, LOW);
    digitalWrite(Motor1_b, LOW);
    digitalWrite(Motor1_c, HIGH);
    digitalWrite(Motor1_d, HIGH);
    break;
    case 2:
    digitalWrite(Motor1_a, LOW);
    digitalWrite(Motor1_b, LOW);
    digitalWrite(Motor1_c, HIGH);
    digitalWrite(Motor1_d, LOW);
    break;
    case 3:
    digitalWrite(Motor1_a, LOW);
    digitalWrite(Motor1_b, HIGH);
    digitalWrite(Motor1_c, HIGH);
    digitalWrite(Motor1_d, LOW);
    break;
    case 4:
    digitalWrite(Motor1_a, LOW);
    digitalWrite(Motor1_b, HIGH);
    digitalWrite(Motor1_c, LOW);
    digitalWrite(Motor1_d, LOW);
    break;
    case 5:
    digitalWrite(Motor1_a, HIGH);
    digitalWrite(Motor1_b, HIGH);
    digitalWrite(Motor1_c, LOW);
    digitalWrite(Motor1_d, LOW);
    break;
    case 6:
    digitalWrite(Motor1_a, HIGH);
    digitalWrite(Motor1_b, LOW);
    digitalWrite(Motor1_c, LOW);
    digitalWrite(Motor1_d, LOW);
    break;
    case 7:
    digitalWrite(Motor1_a, HIGH);
    digitalWrite(Motor1_b, LOW);
    digitalWrite(Motor1_c, LOW);
    digitalWrite(Motor1_d, HIGH);
    break;
    default:
    digitalWrite(Motor1_a, LOW);
    digitalWrite(Motor1_b, LOW);
    digitalWrite(Motor1_c, LOW);
    digitalWrite(Motor1_d, LOW);
    break;
    }
    if (Direction_Cmd == "right") { motorStep++; }
    else if (Direction_Cmd == "left") { motorStep--; }         
    if(motorStep>7) { motorStep=0; }
    if(motorStep<0) { motorStep=7; }
    delay(1);
    return motorStep;
}

double SemiAutoDetection(){
  nextSector = 0;
  int  numSectors = 0;
  double deltaSectors= 0;
  if (digitalRead(ArraySensor1)==HIGH) {
  nextSector+= 1; 
  numSectors++;
  Serial.println("INDEX1 - DETECTED");
  }
  else
    Serial.println("INDEX1 - STATIONARY");   
  if (digitalRead(ArraySensor2)==HIGH) {
    nextSector+=2;
    numSectors++;
    Serial.println("INDEX2 - DETECTED");
  }
  else
    Serial.println("INDEX2 - STATIONARY");
  if (digitalRead(ArraySensor3)==HIGH) { 
    nextSector+=3;
    numSectors++;
    Serial.println("INDEX3 - DETECTED");
  }
  else
    Serial.println("INDEX3 - STATIONARY");
  if(numSectors == 0){
    // Nothing detected
    nextSector = curSector;
    deltaSectors = 0;
  }
  else{
    nextSector = nextSector/numSectors;
    deltaSectors = nextSector-curSector;
  }
  
  Serial.print("Current Sector:");
  Serial.println(curSector);
  Serial.print("Next Sector:");
  Serial.println(nextSector);
  Serial.print("Delta Sector:");
  Serial.println(deltaSectors);
  
  curSector = nextSector;
  return deltaSectors;
}

int AutoRotationStepperMotor_Control(int motorStep, int deltaSectors){   
  switch(motorStep){   
    case 0:      
    digitalWrite(Motor1_a, LOW);
    digitalWrite(Motor1_b, LOW);
    digitalWrite(Motor1_c, LOW);
    digitalWrite(Motor1_d, HIGH);
    break;
    case 1:
    digitalWrite(Motor1_a, LOW);
    digitalWrite(Motor1_b, LOW);
    digitalWrite(Motor1_c, HIGH);
    digitalWrite(Motor1_d, HIGH);
    break;
    case 2:
    digitalWrite(Motor1_a, LOW);
    digitalWrite(Motor1_b, LOW);
    digitalWrite(Motor1_c, HIGH);
    digitalWrite(Motor1_d, LOW);
    break;
    case 3:
    digitalWrite(Motor1_a, LOW);
    digitalWrite(Motor1_b, HIGH);
    digitalWrite(Motor1_c, HIGH);
    digitalWrite(Motor1_d, LOW);
    break;
    case 4:
    digitalWrite(Motor1_a, LOW);
    digitalWrite(Motor1_b, HIGH);
    digitalWrite(Motor1_c, LOW);
    digitalWrite(Motor1_d, LOW);
    break;
    case 5:
    digitalWrite(Motor1_a, HIGH);
    digitalWrite(Motor1_b, HIGH);
    digitalWrite(Motor1_c, LOW);
    digitalWrite(Motor1_d, LOW);
    break;
    case 6:
    digitalWrite(Motor1_a, HIGH);
    digitalWrite(Motor1_b, LOW);
    digitalWrite(Motor1_c, LOW);
    digitalWrite(Motor1_d, LOW);
    break;
    case 7:
    digitalWrite(Motor1_a, HIGH);
    digitalWrite(Motor1_b, LOW);
    digitalWrite(Motor1_c, LOW);
    digitalWrite(Motor1_d, HIGH);
    break;
    default:
    digitalWrite(Motor1_a, LOW);
    digitalWrite(Motor1_b, LOW);
    digitalWrite(Motor1_c, LOW);
    digitalWrite(Motor1_d, LOW);
    break;
    }
    // Rotates in the clockwise sequence by default
    if (deltaSectors>0) { motorStep++; }
    else { motorStep--; }         
    if(motorStep>7) { motorStep=0; }
    if(motorStep<0) { motorStep=7; }
    delay(1);
    return motorStep;
}



