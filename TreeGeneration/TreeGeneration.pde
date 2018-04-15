import g4p_controls.*;
import ddf.minim.*;
import ddf.minim.analysis.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.Iterator;


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

float MAXBRANCHVALUE = .3; //Maximum length a specific branch can be to it's parent. This should be decided more intelligently
float FFTBANDSCALE = 40; //multiplies the size of the heightvalues by this to make them visible... This should be done more intelligently
float FFTDecay = .7;//Rate that everything decays at
float COLORFREQDIVISOR = 50; //Something that modifies the value of the fftband to generate a specific color... this isn't intelligently decided in anyway.
float ANGLEMULTIPLIER = .6;
float BASE = 2.6;



float MaxBandValue = 0; //Current highest observed max value is 818.8453?

final boolean TOPFLOWER = false; //If there's a top flower or not. Note this adds +1 to numberofbranches
int numberoflevels =13; //Should be an even number for tiles on the ground// Bigger than 7 will make things... slow //Has issues when below 6
int numberofbranches = 2;
//Note, this is (numberofbranches)^(numberoflevels+1)... This should be kept under 20000 or so... e.g... 2^14, 3^9, 4^7, 5^6
int fftbands = (int) pow(2, numberoflevels+1); //NOTE This is a 3 when in 3d, and a 2 in 2d...
float[] heights= new float[fftbands];
float[] heights2 = new float[numberoflevels];
color[] colors= new color[fftbands];
float[] angles = new float[fftbands];
int height_index;
int angle_index;
int color_index;

branch b_root; //Treeroot
//branch b_root_2;

//3D STUFF!
float thetaSpeed = .01f;
float currentTheta;

void setup() {
  size(1600, 900, P3D); //NOW IN 3D!!! //CAREFUL, Not what height is being done and stuff! Not everything supports 3d!
  background(0);
  ambientLight(120,120,120);
  createGUI();
  
  surface.setResizable(true);
  minim = new Minim(this);
  b_root = new branch(null, 0, 0);
  //b_root_2 = new branch(null, 0, 0);
  newsession = true;
  currentTheta = 0;
  
  //camera(mouseX, height/2, (height/2) / tan(PI/6), width/2, height/2, 0, 0, 1, 0);

  if (newsession) {
    //Chooses Music File
    JFileChooser chooser = new JFileChooser(sketchPath(""));

    FileNameExtensionFilter filter = new FileNameExtensionFilter("MP3 Files only please","mp3");
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

void draw() {
  if(widthh != width){
   createGUI();
   widthh = width;
   heightt = height;
  }
  background(0);
  frameRate(60);
  
  fft.forward(song.mix); //step the fft to the next song frame

  branch_heights(); //Calculate all branch heights

  branch_angles(); //Calculates all branch angles

  branch_colors(); //Calculates all branch colors

  translate(0, 0, 0);
  //tests(); // generates all testing stuff

  // Start the tree from the bottom of the screen
  translate(width/2, (3*height)/4, -100); // -Z = depth

  currentTheta += thetaSpeed; //Slowly rotate the entire screen
  rotateY(currentTheta);

  pushMatrix();
  b_root.draw_branch();
  popMatrix();
  
  //TILE Generation we should make this more fancy with matricies or something idk dude
  int number_of_tiles = numberoflevels + 1; //How many tiles are on the ground
  float tile_size = 70;
  noFill();//i only want the outline of the rectangles
  for (int x = -number_of_tiles/2; x < number_of_tiles/2; x++) {
    for (int y = -number_of_tiles/2; y < number_of_tiles/2; y++) {
      //run two for loops, cycling through 10 different positions of rectangles
      pushMatrix();
      
      //color k = get_color(heights2[(int) ((sqrt(x*x + y*y) )/2)], 255 - (255 / (number_of_tiles))* abs(sqrt(x*x + y*y)/2));
      color k = get_color(heights2[0]);
      stroke(k);
      
      //uncomment the next line:
      //stroke(0,255,0);
      // to see how the infinity thing works
      
      translate(x*tile_size, 0, y*tile_size);//move the rectangles to where they shall be
      rotateX(HALF_PI);
      rect(0, 0, tile_size, tile_size);
      popMatrix();
    }
    
    if(!song.isPlaying()){
      newsession = true;
    }
  }
  
  
  
}
//return a value that will increase proportionally to interesting data in the fft spectrum.
int get_displaced(int current) {
  //float k = fftbands/numberoflevels;
  //return (int) k*current;
  
  float ln_fft = log(fftbands); 
  return (int) pow(BASE, (float) ((float)current/ (float)numberoflevels)*(ln_fft)/log(BASE)) -1; 
  
}
color random_color() {
  return color((int) random(0, 255), (int) random(0, 255), (int) random(0, 255));
}
color get_color(double freq) {
  freq = freq/COLORFREQDIVISOR;
  return color((int) (Math.sin(freq)* 127 + 128), (int) (Math.sin(freq+((2*Math.PI)/2)) * 127 + 128), (int) (Math.sin(freq+(Math.PI/2)) * 127 + 128));
}
color get_color(double freq, float alpha){
  freq = freq/COLORFREQDIVISOR;
  return color((int) (Math.sin(freq)* 127 + 128), (int) (Math.sin(freq+((2*Math.PI)/2)) * 127 + 128), (int) (Math.sin(freq+(Math.PI/2)) * 127 + 128), alpha);
}

void branch_colors() {
  for (int i = 0; i < heights2.length; i++) {
    colors[i] = get_color(heights2[i] , 200f);
  }
  b_root.set_colors(colors);
  //b_root_2.set_colors(colors);
}

void branch_angles() {
  for (int i = heights2.length -1; i > 0 ; i--) {
    angles[i] = radians(ANGLEMULTIPLIER*heights2[i]);
  }
  b_root.set_angles(angles);
}

void branch_heights() {
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
  //b_root_2.set_height_by_id(heights2);

  //b_root.set_height(heights2, heights, 0, fftbands);
}

void tests() { //Testing Space
  for (int i = 0; i < heights2.length; i++) //tests new heights2 array
  {
    stroke(255, 255, 255, 50);
    fill(140, 140, 140, 50);
    rectMode(CORNERS);
    rect( ((float) width/heights.length)*  get_displaced(i), height, ((float) width/heights.length)*  get_displaced(i+1), height-heights2[i]); //Displays how get_displaced currently scanes the fft groups...May or maynot be aligned to individual spectrum?
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
