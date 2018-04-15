
class branch { //Significes specific branch of an object\
  branch parent;
  branch nextleft;
  branch nextMiddle;
  branch nextright;
  

  int id; //level in the tree

  color b_color;
  float b_height;
  float b_angle;//Z axis up
  float b_offset;//X Axis up
  int level;
  int side;

  branch(branch parent, int side, int id) {
    this.parent = parent;
    this.id = id;
    if (parent != null) {
      this.b_height = .66*parent.b_height;// + //.22*heights[index];//.66*parent.b_height;
      height_index++;
      this.level = parent.level + 1;
      b_angle = THETA;
      b_offset = ((2*PI)/3) * side;
    } else {
      this.b_height = 120; 
      this.level = 0;
      b_angle = 0;
      b_offset = 0;
    }
    this.side = side;
    if (level < numberoflevels) {
      this.nextleft = new branch(this, 0, id +1);
      this.nextMiddle = new branch(this, 1, id +1);
      this.nextright = new branch(this, 2, id +1);
    } else {
      this.nextleft = null;
      this.nextMiddle = null;
      this.nextright = null;
    }

    b_color = color(255,255,255);

    
  }
  void draw_branch() {//float b_height,int input, int leftright){
    pushMatrix();
    rotateX(b_angle);
    //rotateY(side*b_offset);
    //rotateZ(side*b_offset);
    stroke(b_color);
    line(0, 0, 0, -b_height);
    
    if (nextleft != null) {
      translate(0, -b_height, 0);
      pushMatrix();
      rotateY(((2*PI)/3) * 0);
      nextleft.draw_branch();
      popMatrix();
      pushMatrix();
      rotateY(((2*PI)/3) * 1);
      nextMiddle.draw_branch();
      popMatrix();
      pushMatrix();
      rotateY(((2*PI)/3) * 2);
      nextright.draw_branch();
      popMatrix();
    }
    popMatrix();
  }
  //Should these be added? //DOES NOT SUPPORT 3d
  void set_height_by_single_bands(float[] heights, int lower, int upper) {
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

    if (nextleft != null) {
      nextleft.set_height_by_single_bands(heights, lower, (lower+upper)/2);
      //nextleft.set_height_by_single_bands(heights, lower, (lower+upper)/2);
      nextright.set_height_by_single_bands(heights, (lower+upper)/2, upper);
    }
  }

  void set_height_by_id(float[] heights2) { //SUPPORTS 3d!
    if (id < numberoflevels) {
      this.b_height = heights2[id];
    }
    if (nextleft != null) {
      nextleft.set_height_by_id(heights2);
      nextMiddle.set_height_by_id(heights2);
      nextright.set_height_by_id(heights2);
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
  void set_angles(float[] angles) {
    if (parent != null) {
      this.b_angle = angles[angle_index];
    }
    angle_index++;
    if (nextleft != null) {
      nextleft.set_angles(angles);
      nextMiddle.set_angles(angles);
      nextright.set_angles(angles);
    }
  }

  void set_colors(color[] colors) {
    b_color = colors[color_index];
    color_index++;
    if (nextleft != null) {
      nextleft.set_colors(colors);
      //nextMiddle.set_colors(colors);
      nextright.set_colors(colors);
    }
  }
}
