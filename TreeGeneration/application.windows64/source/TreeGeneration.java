import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import g4p_controls.*; 
import ddf.minim.*; 
import ddf.minim.analysis.*; 
import javax.swing.JFileChooser; 
import javax.swing.filechooser.FileNameExtensionFilter; 
import java.util.Iterator; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class TreeGeneration extends PApplet {









//Objects to create trees

//Global test values. These are used before any FFT object are added

boolean newsession;
AudioPlayer song;
Minim minim;
FFT fft;

int widthh = 640;
int heightt = 320;

PVector gridPos = new PVector(0, 0, 0);//the position of the grid (it moves with the camera to make it look infinite)
//UNIVERSAL VALUES
final float THETA = radians(10f); //Universal Testing radians, this needs to not be used in the end

float MAXBRANCHVALUE = .3f; //Maximum length a specific branch can be to it's parent. This should be decided more intelligently
float FFTBANDSCALE = 40; //multiplies the size of the heightvalues by this to make them visible... This should be done more intelligently
float FFTDecay = .7f;//Rate that everything decays at
float COLORFREQDIVISOR = 50; //Something that modifies the value of the fftband to generate a specific color... this isn't intelligently decided in anyway.
float ANGLEMULTIPLIER = .6f;
float BASE = 2.6f;



float MaxBandValue = 0; //Current highest observed max value is 818.8453?

final boolean TOPFLOWER = false; //If there's a top flower or not. Note this adds +1 to numberofbranches
int numberoflevels =12; //Should be an even number for tiles on the ground// Bigger than 7 will make things... slow //Has issues when below 6
int numberofbranches = 2;
//Note, this is (numberofbranches)^(numberoflevels+1)... This should be kept under 20000 or so... e.g... 2^14, 3^9, 4^7, 5^6
int fftbands = (int) pow(2, numberoflevels+1); //NOTE This is a 3 when in 3d, and a 2 in 2d...
float[] heights= new float[fftbands];
float[] heights2 = new float[numberoflevels];
int[] colors= new int[fftbands];
float[] angles = new float[fftbands];
int height_index;
int angle_index;
int color_index;

branch b_root; //Treeroot
branch b_root_2;

//3D STUFF!
float thetaSpeed = .01f;
float currentTheta;

public void setup() {
   //NOW IN 3D!!! //CAREFUL, Not what height is being done and stuff! Not everything supports 3d!
  background(0);
  ambientLight(120, 120, 120);
  createGUI();

  surface.setResizable(true);
  minim = new Minim(this);
  b_root = new branch(null, 0, 0);
  b_root_2 = new branch(null, 0, 0);
  newsession = true;
  currentTheta = 0;

  //camera(mouseX, height/2, (height/2) / tan(PI/6), width/2, height/2, 0, 0, 1, 0);

  if (newsession) {
    //Chooses Music File
    JFileChooser chooser = new JFileChooser(sketchPath(""));

    FileNameExtensionFilter filter = new FileNameExtensionFilter("MP3 Files only please", "mp3");
    chooser.setFileFilter(filter);
    int returnValue = chooser.showOpenDialog(null);
    if (returnValue == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      song = minim.loadFile(file.getAbsolutePath(), fftbands); //fftbands is based on number of levels
    }
    if (song == null) {
      exit();
    }
    song.play();
    newsession = false;
    fft = new FFT(song.bufferSize(), song.sampleRate()); //Begins new fft sessions
  }
}

public void draw() {
  if (widthh != width) {
    createGUI();
    widthh = width;
    heightt = height;
  }
  background(0);
  frameRate(144);

  fft.forward(song.mix); //step the fft to the next song frame

  branch_heights(); //Calculate all branch heights

  branch_angles(); //Calculates all branch angles

  branch_colors(); //Calculates all branch colors

  translate(0, 0, 0);
  tests(); // generates all testing stuff

  // Start the tree from the bottom of the screen
  translate(width/2, (2*height)/4, -100); // -Z = depth

  currentTheta += thetaSpeed; //Slowly rotate the entire screen
  rotateY(currentTheta);

  pushMatrix();
  b_root.draw_branch();
  popMatrix();

  pushMatrix();
  rotateZ(PI);
  b_root_2.draw_branch();
  popMatrix();

  //TILE Generation we should make this more fancy with matricies or something idk dude
  //generate_tiles();

  if (!song.isPlaying()) {
    newsession = true;
  }
}

//return a value that will increase proportionally to interesting data in the fft spectrum.
public int get_displaced(int current) {
  //float k = fftbands/numberoflevels;
  //return (int) k*current;

  float ln_fft = log(fftbands); 
  return ((int)pow(BASE, (float) ((float)(current)/ (float)numberoflevels)*(ln_fft)/log(BASE)));
}
public int random_color() {
  return color((int) random(0, 255), (int) random(0, 255), (int) random(0, 255));
}
public int get_color(double freq) {
  freq = freq/COLORFREQDIVISOR;
  return color((int) (Math.sin(freq)* 127 + 128), (int) (Math.sin(freq+((2*Math.PI)/2)) * 127 + 128), (int) (Math.sin(freq+(Math.PI/2)) * 127 + 128));
}
public int get_color(double freq, float alpha) {
  freq = freq/COLORFREQDIVISOR;
  return color((int) (Math.sin(freq)* 127 + 128), (int) (Math.sin(freq+((2*Math.PI)/2)) * 127 + 128), (int) (Math.sin(freq+(Math.PI/2)) * 127 + 128), alpha);
}

public void branch_colors() {
  for (int i = 0; i < heights2.length; i++) {
    colors[i] = get_color(heights2[i], 200f);
  }
  b_root.set_colors(colors);
  b_root_2.set_colors(colors);
}

public void branch_angles() {
  for (int i = heights2.length -1; i > 0; i--) {
    angles[i] = radians(ANGLEMULTIPLIER*heights2[i]);
  }
  b_root.set_angles(angles);
  b_root_2.set_angles(angles);
}

public void branch_heights() {
  //SETS fft values into a single heights array
  for (int i = 0; i < fftbands; i++) {
    if (heights[i] + FFTBANDSCALE*fft.getBand(i) - FFTDecay > 0) {
      heights[i] += FFTBANDSCALE*fft.getBand(i);
      heights[i] *= FFTDecay;  //Decreases every tick
    } else {
      heights[i] = 0;
    }
  }

  //Version 1 putting the heights into the tree
  /*
   height_index = 0;
   b_root.set_height_by_single_bands((heights,0,fftbands);
   height_index = 0;
   */

  // Version 2, putting the heights into the tree, but by level

  for (int i = 0; i < numberoflevels; i++) { //Create smaller array
    float sum = 0;
    for (int k = get_displaced(i); k < get_displaced(i+1); k++) { //Changes the start and stop values of this index's average. Varies proportionally as index increases
      sum += heights[k];
    }
    sum /= (((fftbands/numberoflevels)*(i))+((fftbands/numberoflevels)*(i+1)))/2; // averages them
    heights2[i] = sum; //multiplies values by something... This needs to be updated
    /*
    if(heights2[i] - FFTDecay < 0){
     heights2[i] = 0;
     }else{
     heights2[i] -= 10;
     }
     */
  }
  b_root.set_height_by_id(heights2);
  b_root_2.set_height_by_id(heights2);

  //b_root.set_height(heights2, heights, 0, fftbands);
}

public void tests() { //Testing Space
  for (int i = 0; i < heights2.length; i++) //tests new heights2 array
  {
    stroke(255, 255, 255, 50);
    fill(140, 140, 140, 50);
    rectMode(CORNERS);
    rect( ((float) width/heights.length)*  get_displaced(i), height, ((float) width/heights.length)*  get_displaced(i+1), height-heights2[i]); //Displays how get_displaced currently scanes the fft groups...May or maynot be aligned to individual spectrum?
    println(get_displaced(i) +" "+ heights.length);
  }

  for (int i = 0; i < heights.length; i++) {
    stroke(255, 0, 0, 150);
    line(((float)width/heights.length)*(i), height, ((float)width/heights.length)*(i), height - fft.getBand(i));
    //println(((float) width/heights.length)*(i+1)+ " : " + height);
  }
  /*
  for (int i = 0; i < fftbands; i++) {
   if (fft.getBand(i) > MaxBandValue) {
   MaxBandValue = fft.getBand(i);
   }
   }
   */
}

public void generate_tiles() {
  int number_of_tiles = numberoflevels + 1; //How many tiles are on the ground
  float tile_size = 70;
  noFill();//i only want the outline of the rectangles
  for (int x = -number_of_tiles/2; x < number_of_tiles/2; x++) {
    for (int y = -number_of_tiles/2; y < number_of_tiles/2; y++) {
      //run two for loops, cycling through 10 different positions of rectangles
      pushMatrix();

      //color k = get_color(heights2[(int) ((sqrt(x*x + y*y) )/2)], 255 - (255 / (number_of_tiles))* abs(sqrt(x*x + y*y)/2));
      int k = get_color(heights2[0]);
      stroke(k);

      //uncomment the next line:
      //stroke(0,255,0);
      // to see how the infinity thing works

      translate(x*tile_size, 0, y*tile_size);//move the rectangles to where they shall be
      rotateX(HALF_PI);
      rect(0, 0, tile_size, tile_size);
      popMatrix();
    }
  }
}

class branch { //Significes specific branch of an object\
  branch parent;
  
  ArrayList<branch>  children = new ArrayList<branch>();

  int id; //level in the tree

  int b_color;
  float b_height;
  float b_angle;//Z axis up
  float b_offset;//X Axis up
  int level;
  int side;

  branch(branch parent, int side, int id) {
    this.parent = parent;
    this.id = id;
    if (parent != null) {
      this.b_height = MAXBRANCHVALUE*parent.b_height;// + //.22*heights[index];//.66*parent.b_height;
      height_index++;
      this.level = parent.level + 1;
      b_angle = THETA;
      if(side == -1){
        b_offset = 0;
        b_angle = 0;
      }else{
        b_offset = ((2*PI)/numberofbranches) * side;
      }
    } else {
      this.b_height = 0; 
      this.level = 0;
      b_angle = 0;
      b_offset = 0;
    }
    this.side = side;
    if (level < numberoflevels) {
      for(int i = 0 - (TOPFLOWER?1:0); i < numberofbranches; i++){
        children.add(new branch(this, i, id+1));
      }
    } else {
      children = null;
    }

    b_color = color(255,255,255);

    
  }
  public void draw_branch() {//float b_height,int input, int leftright){
    pushMatrix();
    rotateX(b_angle);
    //rotateY(side*b_offset);
    //rotateZ(side*b_offset);
    stroke(b_color);
    line(0, 0, 0, -b_height);
    
    if (children != null) {
      translate(0, -b_height, 0);
      for(branch k : children){
        pushMatrix();
        rotateY(((2*PI)/numberofbranches) * k.side);
        k.draw_branch();
        popMatrix();
      }
    }
    popMatrix();
  }
  //Should these be added? //DOES NOT SUPPORT 3d
  public void set_height_by_single_bands(float[] heights, int lower, int upper) {
    if (parent != null) {
      float sum = 0;
      for (int i = lower; i < upper; i++) {
        sum += heights[i];
      }
      if (sum > MAXBRANCHVALUE*parent.b_height) {
        sum = MAXBRANCHVALUE*parent.b_height;
      }
      this.b_height = sum;
    }
    height_index++;

    if (children != null) {
      /*
      children.set_height_by_single_bands(heights, lower, (lower+upper)/2);
      //nextleft.set_height_by_single_bands(heights, lower, (lower+upper)/2);
      nextright.set_height_by_single_bands(heights, (lower+upper)/2, upper);
      */
    }
  }

  public void set_height_by_id(float[] heights2) { //SUPPORTS 3d!
    if (id < numberoflevels) {
      if(id!= 0){
        if(heights2[id] < heights2[id-1]){
          this.b_height = heights2[id];
        }else{
          this.b_height = MAXBRANCHVALUE*heights2[id-1];
        }
      }else{
        //this.b_height = BASE*20 + MAXBRANCHVALUE*heights2[id];
      };    }
    if (children != null) {
      for(branch k : children){
        k.set_height_by_id(heights2);
      }
    }
  }
/*
  void set_height(float[] heights2, float[] heights, int lower, int upper) { //Combines set_by_id, and _by_single_bands //DOESNOT SUPPORT 3d!
    int value = 0;
    if (id < numberoflevels) { //Average for a "level"
      value += heights2[id];
    }
    if(id == numberoflevels){
      float sum = 0; //Average for a "band"
      for (int i = lower; i < upper; i++) {
        sum += heights[i];
      }
      sum/= (lower + upper)/2;
      value += sum;
      height_index++;
    }
    b_height = value;
    if (nextleft != null) {
      nextleft.set_height(heights2, heights, lower, (lower+upper)/2);
      nextright.set_height(heights2, heights, (lower+upper)/2, upper);
    }
  }
*/
  public void set_angles(float[] angles) {
    if(side == -1){
      b_angle = 0;
    }else{
      b_angle = angles[id];
    }
    if (children != null) {
      for(branch k : children){
        k.set_angles(angles);
      }
    }
  }

  public void set_colors(int[] colors) {
    b_color = colors[id];
    //color_index++;
    if (children != null) {
      for(branch k : children){
        k.set_colors(colors);
      }
    }
  }
}
/* =========================================================
 * ====                   WARNING                        ===
 * =========================================================
 * The code in this tab has been generated from the GUI form
 * designer and care should be taken when editing this file.
 * Only add/edit code inside the event handlers i.e. only
 * use lines between the matching comment tags. e.g.

 void myBtnEvents(GButton button) { //_CODE_:button1:12356:
     // It is safe to enter your event code here  
 } //_CODE_:button1:12356:
 
 * Do not rename this tab!
 * =========================================================
 */

public void panel1_Click1(GPanel source, GEvent event) { //_CODE_:panel1:490010:
  println("panel1 - GPanel >> GEvent." + event + " @ " + millis());
} //_CODE_:panel1:490010:

public void BranchValueSliderChange(GSlider source, GEvent event) { //_CODE_:BranchValueSlider:727545:
  //println("slider1 - GSlider >> GEvent." + event + " @ " + millis());
  MAXBRANCHVALUE = BranchValueSlider.getValueF();
} //_CODE_:BranchValueSlider:727545:

public void FFTBandsScaleSlider_change1(GSlider source, GEvent event) { //_CODE_:FFTBandsScaleSlider:835083:
   FFTBANDSCALE = FFTBandsScaleSlider.getValueF();
} //_CODE_:FFTBandsScaleSlider:835083:

public void FFTDecaySlider_change1(GSlider source, GEvent event) { //_CODE_:FFTDecaySlider:487922:
  FFTDecay = FFTDecaySlider.getValueF();
} //_CODE_:FFTDecaySlider:487922:

public void COLORFREQDIVISORSlider_change1(GSlider source, GEvent event) { //_CODE_:COLORFREQDIVISORSlider:557796:
  COLORFREQDIVISOR = COLORFREQDIVISORSlider.getValueF();
} //_CODE_:COLORFREQDIVISORSlider:557796:

public void ANGLEMULTIPLIERSlider_change1(GSlider source, GEvent event) { //_CODE_:ANGLEMULTIPLIERSlider:738625:
  ANGLEMULTIPLIER = ANGLEMULTIPLIERSlider.getValueF();
} //_CODE_:ANGLEMULTIPLIERSlider:738625:

public void BASESlider_change1(GSlider source, GEvent event) { //_CODE_:BASESlider:303729:
  BASE = BASESlider.getValueF();
} //_CODE_:BASESlider:303729:



// Create all the GUI controls. 
// autogenerated do not edit
public void createGUI(){
  G4P.messagesEnabled(false);
  G4P.setGlobalColorScheme(GCScheme.BLUE_SCHEME);
  G4P.setCursor(ARROW);
  surface.setTitle("Sketch Window");
  panel1 = new GPanel(this, 1440, 10, 150, 200, "Variables");
  panel1.setCollapsible(false);
  panel1.setDraggable(false);
  panel1.setText("Variables");
  panel1.setOpaque(false);
  panel1.addEventHandler(this, "panel1_Click1");
  BranchValueSlider = new GSlider(this, 0, 40, 150, 10, 10.0f);
  BranchValueSlider.setShowValue(true);
  BranchValueSlider.setShowLimits(true);
  BranchValueSlider.setLimits(0.66f, 0.0f, 1.0f);
  BranchValueSlider.setShowTicks(true);
  BranchValueSlider.setNumberFormat(G4P.DECIMAL, 2);
  BranchValueSlider.setOpaque(false);
  BranchValueSlider.addEventHandler(this, "BranchValueSliderChange");
  MaxBranchLable = new GLabel(this, 0, 20, 130, 20);
  MaxBranchLable.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
  MaxBranchLable.setText("MAXBRANCHVALUE");
  MaxBranchLable.setOpaque(false);
  FFTBANDSCALELable = new GLabel(this, 0, 50, 130, 20);
  FFTBANDSCALELable.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
  FFTBANDSCALELable.setText("FFTBANDSCALE");
  FFTBANDSCALELable.setOpaque(false);
  FFTBandsScaleSlider = new GSlider(this, 0, 70, 150, 10, 10.0f);
  FFTBandsScaleSlider.setShowValue(true);
  FFTBandsScaleSlider.setShowLimits(true);
  FFTBandsScaleSlider.setLimits(40.0f, 1.0f, 200.0f);
  FFTBandsScaleSlider.setShowTicks(true);
  FFTBandsScaleSlider.setNumberFormat(G4P.DECIMAL, 2);
  FFTBandsScaleSlider.setOpaque(false);
  FFTBandsScaleSlider.addEventHandler(this, "FFTBandsScaleSlider_change1");
  FFTDecaylable = new GLabel(this, 0, 80, 130, 20);
  FFTDecaylable.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
  FFTDecaylable.setText("FFTDECAY");
  FFTDecaylable.setOpaque(false);
  FFTDecaySlider = new GSlider(this, 0, 100, 150, 10, 10.0f);
  FFTDecaySlider.setShowValue(true);
  FFTDecaySlider.setShowLimits(true);
  FFTDecaySlider.setLimits(0.7f, 0.0f, 1.0f);
  FFTDecaySlider.setShowTicks(true);
  FFTDecaySlider.setNumberFormat(G4P.DECIMAL, 2);
  FFTDecaySlider.setOpaque(false);
  FFTDecaySlider.addEventHandler(this, "FFTDecaySlider_change1");
  COLORFREQDIVISORLable = new GLabel(this, 0, 110, 130, 20);
  COLORFREQDIVISORLable.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
  COLORFREQDIVISORLable.setText("COLORFREQDIVISOR");
  COLORFREQDIVISORLable.setOpaque(false);
  COLORFREQDIVISORSlider = new GSlider(this, 0, 130, 150, 10, 10.0f);
  COLORFREQDIVISORSlider.setShowValue(true);
  COLORFREQDIVISORSlider.setShowLimits(true);
  COLORFREQDIVISORSlider.setLimits(50.0f, 1.0f, 900.0f);
  COLORFREQDIVISORSlider.setShowTicks(true);
  COLORFREQDIVISORSlider.setNumberFormat(G4P.DECIMAL, 2);
  COLORFREQDIVISORSlider.setOpaque(false);
  COLORFREQDIVISORSlider.addEventHandler(this, "COLORFREQDIVISORSlider_change1");
  ANGLEMULTIPLIERLabel = new GLabel(this, 0, 140, 130, 20);
  ANGLEMULTIPLIERLabel.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
  ANGLEMULTIPLIERLabel.setText("ANGLEMULTIPLIER");
  ANGLEMULTIPLIERLabel.setOpaque(false);
  ANGLEMULTIPLIERSlider = new GSlider(this, 0, 160, 150, 10, 10.0f);
  ANGLEMULTIPLIERSlider.setShowValue(true);
  ANGLEMULTIPLIERSlider.setShowLimits(true);
  ANGLEMULTIPLIERSlider.setLimits(0.6f, 0.0f, 1.4f);
  ANGLEMULTIPLIERSlider.setShowTicks(true);
  ANGLEMULTIPLIERSlider.setNumberFormat(G4P.DECIMAL, 2);
  ANGLEMULTIPLIERSlider.setOpaque(false);
  ANGLEMULTIPLIERSlider.addEventHandler(this, "ANGLEMULTIPLIERSlider_change1");
  BASELabel = new GLabel(this, 0, 170, 130, 20);
  BASELabel.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
  BASELabel.setText("BASE bin divisor");
  BASELabel.setOpaque(false);
  BASESlider = new GSlider(this, 0, 190, 150, 10, 10.0f);
  BASESlider.setShowValue(true);
  BASESlider.setShowLimits(true);
  BASESlider.setLimits(2.7f, 1.1f, 20.0f);
  BASESlider.setShowTicks(true);
  BASESlider.setNumberFormat(G4P.DECIMAL, 2);
  BASESlider.setOpaque(false);
  BASESlider.addEventHandler(this, "BASESlider_change1");
  panel1.addControl(BranchValueSlider);
  panel1.addControl(MaxBranchLable);
  panel1.addControl(FFTBANDSCALELable);
  panel1.addControl(FFTBandsScaleSlider);
  panel1.addControl(FFTDecaylable);
  panel1.addControl(FFTDecaySlider);
  panel1.addControl(COLORFREQDIVISORLable);
  panel1.addControl(COLORFREQDIVISORSlider);
  panel1.addControl(ANGLEMULTIPLIERLabel);
  panel1.addControl(ANGLEMULTIPLIERSlider);
  panel1.addControl(BASELabel);
  panel1.addControl(BASESlider);
}

// Variable declarations 
// autogenerated do not edit
GPanel panel1; 
GSlider BranchValueSlider; 
GLabel MaxBranchLable; 
GLabel FFTBANDSCALELable; 
GSlider FFTBandsScaleSlider; 
GLabel FFTDecaylable; 
GSlider FFTDecaySlider; 
GLabel COLORFREQDIVISORLable; 
GSlider COLORFREQDIVISORSlider; 
GLabel ANGLEMULTIPLIERLabel; 
GSlider ANGLEMULTIPLIERSlider; 
GLabel BASELabel; 
GSlider BASESlider; 
  public void settings() {  size(1600, 900, P3D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "TreeGeneration" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
