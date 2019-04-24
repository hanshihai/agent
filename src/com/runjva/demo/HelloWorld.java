package com.runjva.demo;

public class HelloWorld {
	
public String sayHello(String s){
	System.out.println("input : "+s);
	return s;
}
  public static void main(String args[]) {
    HelloWorld hw = new HelloWorld();
    String result = hw.sayHello("World");
    System.out.println("result : "+result);
  }
}
