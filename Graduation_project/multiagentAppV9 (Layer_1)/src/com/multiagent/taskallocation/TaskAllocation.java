/**
 * 
 * @author Mahmoud Talaat Rady
 *
 *we need to assign the task for the best available robot
 *
 *We could consider the inputs as 1-no_Robot
 *								  2-Map Size (X & Y)
 *								  3-start_X , start_Y
 *								  4-end_X , end_Y
 *
 */
package com.multiagent.taskallocation;

import c.beans.robot;
import db.connection.mySQLConnUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import com.multiagent.pathplanning.PathPlanning;
import com.multiagent.robot.RobotUtils;

import java.io.IOException;
import java.lang.*;
import java.sql.Connection;
import java.sql.SQLException;

public class TaskAllocation 
{		
	static boolean TaskAlloBusy;
	public static void setTaskAlloBusy()
	{
		TaskAlloBusy=false;
	}

    static List<String> dataToRead = new ArrayList<>();

//	final static int no_Robot=3;            	     //No Of Robots in map
	final static int X=19;            	     		//Map size in X
	final static int Y=19;            	   		   //Map size in Y
										//this Locations Could be input for the class

	static int feedpos_X=0;            		   //Feedback from the raspberry
	static int feedpos_Y=0;            		   //Feedback from the raspberry
	
	

	static Vector<Integer> Vx = new Vector<Integer>();					   //the position vector for x generated by path planing
	static Vector<Integer> Vy = new Vector<Integer>();					  //the position vector for y generated by path planing
	
	public static List<String>  taskAllocationFn(String writeFile, String exePath, String readFile,
			int noOfAllRobot, int start_X, int start_Y, int end_X, int end_Y, int noOfAgent) throws ClassNotFoundException, SQLException, IOException 
	{
		while(TaskAlloBusy!=false)
		{
			System.out.println("Stuck task allocation");
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			continue;
		}
		TaskAlloBusy=true;
		System.out.println("TaskAlloBusy= " + TaskAlloBusy);
		System.out.println("task allocation");
		List<String> list_Robot_IP = new ArrayList<>();				   //The allocated robots for the task
		
		int[] arr_Available = new int[noOfAllRobot];        		 //array for Robots to know which is available
		int[] arr_State = new int[noOfAllRobot];					//array for each robot state 0 ,1 ,2 or 3
	    int[][] arr_Parking = new int[noOfAllRobot][2];			   //array of the parking Location for each Robot
	    int[] arr_ID = new int[noOfAllRobot];              		  //array of the ID
	    List<String> list_IP = new ArrayList<>();				 //list of the IP
		//---------------------------------------------------------------------------------------------------------------------
	    int[] old_arr_Available = new int[noOfAllRobot];                  //array for Robots to know which is available
	    int[] old_arr_State = new int[noOfAllRobot];                     //array for each robot state 0 ,1 ,2 or 3
	    int[][] old_arr_Parking = new int[noOfAllRobot][2];             //array of the parking Location for each Robot
	    int[] old_arr_ID = new int[noOfAllRobot];               	   //array ID
	     List<String> old_list_IP = new ArrayList<>();				  //list of the IP
	    //---------------------------------------------------------------------------------------------------------------------
	     
		Connection conn = mySQLConnUtils.getMySQLConnection();
		List<robot> robotPos = RobotUtils.getAvailableRobot(conn);

		int cnt_Available = 0;    				     //Number of available robots
		int thebestrobot;   						//The best robot number
		int theminpath=100000;					   //the min path length
		int temp;                                 //use this to compare each path length with another one
		int robot = 0;							 //the best robot that we need to assign the task to
		int flagoutofparking=0;					//his value became 1 if there's any robot stop out of his parking area
		List<robot> r = RobotUtils.getRobotData(conn);
		int countList = r.size();
		thebestrobot=TaskAllocationUtils.getTheBestRobotNumber(Y, start_Y, noOfAllRobot);
		System.out.println("The thebestrobot value is " + thebestrobot);
		 //from other code replace by old begin from 114 to 160 140 -->153
		for(int i=0 ; i< countList ; i++)
        {
            //connect to database to get the availability ,the parking location and state of each robot.
            // count the number of available robots (cnt_Available)
            //the next assignment instead of getting this data form here
           
            old_arr_ID[i]=r.get(i).getR_ID();
            old_list_IP.add(r.get(i).getR_IP());
            old_arr_Available[i]=r.get(i).getAVAILITY();
            old_arr_State[i] = r.get(i).getstatus();
            old_arr_Parking[i][0] = r.get(i).getparkingX();
            old_arr_Parking[i][1] = r.get(i).getparkingY();
            if(old_arr_Available[i] == 1)
            {
                cnt_Available++;
            }
        }
       
        //-----------------------------------------Connector------------------------------------------------
        //to sort the data from db with respect to Y location
        //--------------------------------------------------------------------------------------------------
        int c=0;
        Vector<Integer> sortcount = new Vector<Integer>();
        for (int i=0; i<noOfAllRobot; i++)
        {
             c=0;
             for (int j=0; j<noOfAllRobot; j++)
             {
                 if(old_arr_Parking[i][1]>old_arr_Parking[j][1])
                 {
                     c++;
                 }
             }
             sortcount.addElement(c);
         }
        //--------------------------------------------------------------------------------------------------
        for (int i=0,j; i<sortcount.size(); i++)
        {  
            j=sortcount.get(i);
            list_IP.add(old_list_IP.get(j));
            arr_ID[j]=old_arr_ID[i];
            arr_Available[j]=old_arr_Available[i];
            arr_State[j]=old_arr_State[i];
            arr_Parking[j][0]=old_arr_Parking[i][0];
            arr_Parking[j][1]=old_arr_Parking[i][1];
        }
        //--------------------------------------------------------------------------------------------------
        //--------------------------------------------------------------------------------------------------
		for(int i=0;i<arr_Parking.length; i++ ){
			for(int j=0 ; j<2; j++){
			System.out.println("park: "+ arr_Parking[i][j]);
			}
		}
		
		// to return if there is no enough robots
		if( cnt_Available < noOfAgent)
		{
		list_Robot_IP.add("-1");
		return list_Robot_IP;
		}
		
		
		for(int noR=0 ; noR <noOfAgent ; noR++ )
		{
//-------------------------------------------------------------------------------------------------------------
		
		//if the best robot is the first or the last so only check for 2	
		if(thebestrobot==1)
		{
			System.out.println("i am here best 1");
			if(arr_State[thebestrobot-1]==3)
			{
				//stop the robot in his position and get his position
				//change a flag here ,cause there's a robot that isn't stopping in his parking
				feedpos_X=6;
				feedpos_Y=3;
				
				flagoutofparking=1;
				//insert his position(x,y) and the start location(x,y) into the input file
				
				String dataToWrite = TaskAllocationUtils.dataWrite(feedpos_X, feedpos_Y, start_X, start_Y);
				TaskAllocationUtils.writeAndPath(writeFile, dataToWrite, exePath, 1);
				
				//get the position matrix from the output file of the path planning ----> toqa
				dataToRead = PathPlanning.readFromFile(readFile);
				temp = TaskAllocationUtils.getPathLength(dataToRead);
				if(theminpath > temp)
				{
					theminpath= temp;
					robot=thebestrobot;
				}
			}
			for(int i=0; i<2 ;i++)
			{
				if(arr_Available[i] == 1)
				{
					//from the matrix we get above for poses
					int posX = arr_Parking[i][0];
					int posY = arr_Parking[i][1];
					// insert the parking location(x,y) and the start location(x,y) into the input file
					//call the path planning function to get the path
					String dataToWrite = TaskAllocationUtils.dataWrite(posX, posY, start_X, start_Y);
					TaskAllocationUtils.writeAndPath(writeFile, dataToWrite, exePath, 1);
					// get the position matrix from the output file of the path planning
					dataToRead = PathPlanning.readFromFile(readFile);
					temp = TaskAllocationUtils.getPathLength(dataToRead);
					
					if(theminpath > temp)
					{
						theminpath= temp;
						robot=i+1;
					}
				}
			}
		}
		
		else if(thebestrobot==noOfAllRobot)
		{
			if(arr_State[thebestrobot-1]==3)
			{
				feedpos_X=16;
				feedpos_Y=16;

				//stop the robot in his position and get his position -->?
				//change a flag here ,cause there's a robot that isn't stopping in his parking
				flagoutofparking=1;
				//insert his position(x,y) and the start location(x,y) into the input file
				//call the path planning function to get the path
				String dataToWrite = TaskAllocationUtils.dataWrite(feedpos_X, feedpos_Y, start_X, start_Y);
				TaskAllocationUtils.writeAndPath(writeFile, dataToWrite, exePath, 1);

				//get the position matrix from the output file of the path planning
				dataToRead = PathPlanning.readFromFile(readFile);

				temp = TaskAllocationUtils.getPathLength(dataToRead);
				if(theminpath > temp)
				{
					theminpath= temp;
					robot=thebestrobot;
				}
			}
			for(int i=0; i<2 ;i++)
			{
				if(arr_Available[noOfAllRobot-1-i] == 1)
				{
					//arr_parking index ---> no_Robot-1-i
					System.out.println("The robot number= " + (noOfAllRobot-i));
					System.out.println("The robot posX= " + arr_Parking[noOfAllRobot-1-i][0]);
					System.out.println("The robot posY= " + arr_Parking[noOfAllRobot-1-i][1]);
					int posX = arr_Parking[noOfAllRobot-1-i][0];
					int posY = arr_Parking[noOfAllRobot-1-i][1];
					// insert the parking location(x,y) and the start location(x,y) into the input file
					//call the path planning function to get the path
					String dataToWrite = TaskAllocationUtils.dataWrite(posX, posY, start_X, start_Y);
					TaskAllocationUtils.writeAndPath(writeFile, dataToWrite, exePath, 1);
					// get the position matrix from the output file of the path planning
					dataToRead = PathPlanning.readFromFile(readFile);
					temp = TaskAllocationUtils.getPathLength(dataToRead);
					if(theminpath > temp)
					{
						theminpath= temp;
						robot=noOfAllRobot-i;
					}
				}
			}
		}
		
		//if the best robot is not the first and not the last ,so w need to check 3 times
		else
		{
			if(arr_State[thebestrobot-1]==3)
			{
				//feedback
				int posX = 12;
				int posY = 6;

				//stop the robot in his position and get his position
				//change a flag here ,cause there's a robot that isn't stopping in his parking
				flagoutofparking=1;
				//insert his position(x,y) and the start location(x,y) into the input file
				//call the path planning function to get the path
				String dataToWrite = TaskAllocationUtils.dataWrite(posX, posY, start_X, start_Y);
				TaskAllocationUtils.writeAndPath(writeFile, dataToWrite, exePath, 1);

				//get the position matrix from the output file of the path planning
				dataToRead = PathPlanning.readFromFile(readFile);
				
				temp = TaskAllocationUtils.getPathLength(dataToRead);
				if(theminpath > temp)
				{
					theminpath= temp;
					robot=thebestrobot;
				}
			}
			for(int i=1; i<=3 ;i++)
			{
				//if the best is number 1 so [   2+1-1=(2) 2+1-2=(1) 2+1-3=(0)   ] and so on
				if(arr_Available[thebestrobot+1-i] == 1)
				{

					int posX = arr_Parking[thebestrobot+1-i][0];
					int posY = arr_Parking[thebestrobot+1-i][1];
					// insert the parking location(x,y) and the start location(x,y) into the input file
					//call the path planning function to get the path
					String dataToWrite = TaskAllocationUtils.dataWrite(posX, posY, start_X, start_Y);
					TaskAllocationUtils.writeAndPath(writeFile, dataToWrite, exePath, 1);

					// get the position matrix from the output file of the path planning
					dataToRead = PathPlanning.readFromFile(readFile);
					temp = TaskAllocationUtils.getPathLength(dataToRead);
					if(theminpath > temp)
					{
						theminpath= temp;
						robot=thebestrobot+2-i;
					}
				}
			}
		}
		
		if(robot != thebestrobot && flagoutofparking==1)
		{
			//move thebestrobot to his parking 
			//call path planning
			//change a flag here ,cause there's a robot that isn't stopping in his parking
			flagoutofparking=0;
		}
		
		if(robot==0)
		{
		for(int i=1; i<=3 ;i++)
		{
			if(arr_Available[i-1] == 1)
			{
				robot=i;
			}
		}
		}
		
		//must have at least one available to continue
		//while(cnt_Available == 0)
		//{
		// call task allocation again
		//}		
		
		list_Robot_IP.add( list_IP.get(robot-1) );
		//change availability = 0 in the database
		RobotUtils.updateRobotAvailabilityByIp(list_IP.get(robot-1),0);
		System.out.println("Befor"+arr_Available[robot-1]);
		arr_Available[robot-1]=0;
		theminpath=100000;
		System.out.println("after"+arr_Available[robot-1]);
		//
		
	System.out.println("The best robot is " + robot);
	System.out.println("Robot IP " + list_IP.get(robot-1));
	System.out.println("    StartPoint= " + start_X +", " + start_Y + "    EndPoint= " + end_X +", " + end_Y  + "   -> " +list_IP.get(robot-1));
	//we could change the name of the main function to taskAllocation and make it return int(robot number to be assigned)
	//return robot ;
		}
	
	String a;
	if( noOfAgent == 2)
	{
		a=list_Robot_IP.get(0);
		list_Robot_IP.set(0, list_Robot_IP.get(1));
		list_Robot_IP.set(1, a);
	}
	if( noOfAgent == 3)
	{
		if(thebestrobot==2)
		{
			a=list_Robot_IP.get(0);
			list_Robot_IP.set(0, list_Robot_IP.get(1));
			list_Robot_IP.set(1, a);
			
			a=list_Robot_IP.get(1);
			list_Robot_IP.set(1, list_Robot_IP.get(2));
			list_Robot_IP.set(2, a);
		}
		else
		{
			a=list_Robot_IP.get(0);
			list_Robot_IP.set(0, list_Robot_IP.get(2));
			list_Robot_IP.set(2, a);
		}
	}
		
	System.out.println("Task Robot IPs " + list_Robot_IP);	
	TaskAlloBusy=false;
	return list_Robot_IP;
	}
	
//	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException{
//		
//		String writeFile = "C:\\Users\\toqa_\\Desktop\\V10\\Input_Start_and_End_Points.txt" ;
//		String exePath = "F:\\ACMTraining2017\\TestPathPlanning\\Debug\\TestPathPlanning.exe";
//		String readFile = "C:\\Users\\toqa_\\Desktop\\V10\\\\Output_Position_matrix.txt";
//		System.out.println(taskAllocationFn(writeFile, exePath, readFile, 3, 10, 10, 17, 17, 1));
//		
//	}

}