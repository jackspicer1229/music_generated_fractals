//Objects to create trees

//Global test values. These are used before any FFT object are added
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
  b_head = new branch(null,0);
}

void draw(){
  background(0);
  frameRate(30);
  stroke(255);
  
  // Start the tree from the bottom of the screen
  translate(width/2,height);
  
  //Random Heights
  for(int i = 0; i < (int) pow(2,numberoflevels+1); i++){
    heights[i] = random(.5,.9);
  }
  height_index = 0;
  b_head.set_height(heights);
  height_index = 0;
  //
  //Random Heights
  for(int i = 0; i < (int) pow(2,numberoflevels+1); i++){
    angles[i] = (360/(2*PI))*(random(0,90));
  }
  angle_index = 0;
  b_head.set_angles(angles);
  angle_index = 0;
  
  //Random Colors
  for(int i = 0; i < (int) pow(2,numberoflevels+1); i++){
    colors[i] = random_color();
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

class branch{ //Significes specific branch of an object\
  branch parent;
  branch nextleft;
  branch nextright;
  
  color b_color;
  float b_height;
  float b_angle;
  int level;
  int side;
  
  branch(branch parent, int side){
    this.parent = parent;
    if(parent != null){
      this.b_height = .66*parent.b_height;// + //.22*heights[index];//.66*parent.b_height;
      height_index++;
      this.level = parent.level + 1;
    }else{
       this.b_height = 120; 
       this.level = 0;
    }
    this.side = side;
    if(level < numberoflevels){
      this.nextleft = new branch(this, -1);
      this.nextright = new branch(this, 1);
    }else{
       this.nextleft = null;
       this.nextright = null;
    }
    b_angle = 0;
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
  void set_height(float[] heights){
   if(parent != null){
     this.b_height = parent.b_height* heights[height_index];
   }
   height_index++;
   if(nextleft != null){
     nextleft.set_height(heights);
     nextright.set_height(heights);
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
   if(parent != null){
     b_color = colors[color_index];
     
   }
   color_index++;
   if(nextleft != null){
     nextleft.set_colors(colors);
     nextright.set_colors(colors);
   }
  }
}
