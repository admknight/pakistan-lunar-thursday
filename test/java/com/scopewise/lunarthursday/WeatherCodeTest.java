package com.scopewise.lunarthursday;
public final class WeatherCodeTest {
  public static void main(String[] args) {
    int[][] cases={{0,0},{1,0},{2,1},{3,1},{45,2},{48,2},{51,3},{55,3},{61,3},{67,3},{71,4},{75,4},{77,4},{80,3},{82,3},{85,4},{86,4},{95,5},{99,5},{100,1}};
    for(int[] x:cases)if(WeatherCode.category(x[0])!=x[1])throw new AssertionError("WMO "+x[0]);
    System.out.println("WeatherCodeTest PASS: "+cases.length+" cases");
  }
}
