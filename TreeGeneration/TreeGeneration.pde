import ddf.minim.*;
import ddf.minim.analysis.*;
import javax.swing.JFileChooser;
import java.util.Iterator;

//Objects to create trees

//Global test values. These are used before any FFT object are added

boolean newsession;
AudioPlayer song;
Minim minim;
FFT fft;

float theta = radians(45f);   

int numberoflevels =10;
float[] heights= new float[(int) pow(2,numberoflevels+1)];
color[] colors= new color[(int) pow(2,numberoflevels+1)];
float[] angles = new float[(int) pow(2,numberoflevels+1)];
int height_index;
int angle_index;
int color_index;
branch b_head;

void setup(){
  size(640, 360);
   minim = new Minim(this);
  b_head = new branch(null,0,0);
  newsession = true;
}

void draw(){
  if(newsession){
    JFileChooser chooser = new JFileChooser();
    int returnValue = chooser.showOpenDialog(null);
    if(returnValue == JFileChooser.APPROVE_OPTION){
      File file = chooser.getSelectedFile();
      song = minim.loadFile(file.getAbsolutePath(),(int) pow(2,numberoflevels+1));  
    }
    song.play();
    newsession = false;
    fft = new FFT(song.bufferSize(), song.sampleRate());
    println("HELLO" + song.bufferSize()+"YA"+ song.sampleRate() +" YA" + song.sampleRate()/song.bufferSize() );
  }
  
  fft.forward(song.mix);
  
  //println(fft.specSize());
  
  background(0);
  frameRate(30);
  stroke(255);
  
  // Start the tree from the bottom of the screen
  translate(width/2,height);
  float fftave = 0;
  for(int c = 0; c < fft.specSize(); c++){
     fftave+=fft.getBand(c); 
  }
  println(fftave);
  //Random Heights
  
  for(int i = 0; i < (int) pow(2,numberoflevels+1); i++){
    heights[i] = fft.getBand(i);//random(.5,.9);
  }
  /*
  height_index = 0;
  b_head.set_height(heights,0,(int) pow(2,numberoflevels+1));
  height_index = 0;
  */
  
  float[] heights2 = new float[numberoflevels];
  int numHeights = (int) pow(2,numberoflevels+1);
  for(int i = 0; i < numberoflevels; i++){
    float sum = 0;
    for(int k = (numHeights/numberoflevels)*i; k < (numHeights/numberoflevels)*(i+1); k++){
     sum += heights[k]; 
    }
    sum /= (((numHeights/numberoflevels)*(i))+((numHeights/numberoflevels)*(i+1)))/2;
    heights2[i] = sum;
  }
  
  b_head.set_height_by_id(heights2);
  //
  /*
  for(int i = 0; i < (int) pow(2,numberoflevels+1); i++){
    angles[i] = (360/(2*PI))*(100);//random(0,90));
  }
  angle_index = 0;
  b_head.set_angles(angles);
  angle_index = 0;
  */
  //Random Colors
  for(int i = 0; i < (int) pow(2,numberoflevels+1); i++){
    colors[i] = get_color(fft.getBand(i));
  }
  color_index = 0;
  b_head.set_colors(colors);
  color_index = 0;
  
  b_head.draw_branch();
  
  delay(10);
}

color random_color(){
 return color((int) random(0,255), (int) random(0,255), (int) random(0,255));
}
color get_color(double freq){
    return color((int) (Math.sin(freq)* 127 + 128), (int) (Math.sin(freq+((2*Math.PI)/2)) * 127 + 128), (int) (Math.sin(freq+(Math.PI/2)) * 127 + 128));
}

class branch{ //Significes specific branch of an object\
  branch parent;
  branch nextleft;
  branch nextright;
  
  int id;
  
  color b_color;
  float b_height;
  float b_angle;
  int level;
  int side;
  
  branch(branch parent, int side, int id){
    this.parent = parent;
    this.id = id;
    if(parent != null){
      this.b_height = .66*parent.b_height;// + //.22*heights[index];//.66*parent.b_height;
      height_index++;
      this.level = parent.level + 1;
      b_angle = theta;
    }else{
       this.b_height = 120; 
       this.level = 0;
       b_angle = 0;
    }
    this.side = side;
    if(level < numberoflevels){
      this.nextleft = new branch(this, -1, id +1);
      this.nextright = new branch(this, 1, id +1);
    }else{
       this.nextleft = null;
       this.nextright = null;
    }
    
    b_color = color(0,0,0);
  }
  void draw_branch(){//float b_height,int input, int leftright){
    pushMatrix();
    rotate(side*b_angle);
    stroke(b_color);
    line(0,0,0,-b_height);
    if(nextleft != null){
      translate(0, -b_height);
      nextleft.draw_branch();
      nextright.draw_branch();
    }
    popMatrix();
  }
  void set_height(float[] heights,int lower,int upper){
   if(parent != null){
     float sum = 0;
     for(int i = lower; i < upper; i++){
      sum += heights[i]; 
     }
     if(sum > .66*parent.b_height){
       sum = .66*parent.b_height;
     }
     this.b_height = sum;
   }
   height_index++;
   if(nextleft != null){
     nextleft.set_height(heights,lower, (lower+upper)/2);
     nextright.set_height(heights,(lower+upper)/2, upper);
   }
  }
  void set_angles(float[] angles){
   if(parent != null){
     this.b_angle = angles[angle_index];
   }
   angle_index++;
   if(nextleft != null){
     nextleft.set_angles(angles);
     nextright.set_angles(angles);
   }
  }
  void set_colors(color[] colors){
   b_color = colors[color_index];
   color_index++;
   if(nextleft != null){
     nextleft.set_colors(colors);
     nextright.set_colors(colors);
   }
  }
  
  void set_height_by_id(float[] heights2){
    if(id < numberoflevels){
      this.b_height = heights2[id];
    }
    if(nextleft != null){
      nextleft.set_height_by_id(heights2);
      nextright.set_height_by_id(heights2);
    }
  }
}
