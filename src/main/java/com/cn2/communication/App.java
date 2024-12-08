package com.cn2.communication;

import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import java.lang.Thread;

public class App extends Frame implements WindowListener, ActionListener {

	/*
	 * Definition of the app's fields
	 */
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;				
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";	
	
	/**
	 * Construct the app's frame and initialize important parameters
	 */
	public App(String title) {
		
		/*
		 * 1. Defining the components of the GUI
		 */
		
		// Setting up the characteristics of the frame
		super(title);									
		gray = new Color(254, 254, 254);		
		setBackground(gray);
		setLayout(new FlowLayout());			
		addWindowListener(this);	
		
		// Setting up the TextField and the TextArea
		inputTextField = new TextField();
		inputTextField.setColumns(20);
		
		// Setting up the TextArea.
		textArea = new JTextArea(10,40);			
		textArea.setLineWrap(true);				
		textArea.setEditable(false);			
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		//Setting up the buttons
		sendButton = new JButton("Send");		
						
		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);								
		add(inputTextField);
		add(sendButton);
		
		/*
		 * 3. Linking the buttons to the ActionListener
		 */
		sendButton.addActionListener(this);		

		
	}
	
	/**
	 * The main method of the application. It continuously listens for
	 * new messages.
	 */
	public static void main(String[] args){
	
		/*
		 * 1. Create the app's window
		 */
		App app = new App("Chat and call over TCP");																	  
		app.setSize(500,250);				  
		app.setVisible(true);				  

		/*
		 * 2. 
		 */
			new Thread(() -> {
		        	try {
		        		
		        		ServerSocket serverSocket = new ServerSocket(5001);
		        				        		
		                while (true) {
		                	
		                    Socket clientSocket = serverSocket.accept();
		                    
		                    
		                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	                        String clientMessage = in.readLine();
		                    
		                    DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
		                    DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());

		                    textArea.append("Anatoli: " + clientMessage + newline);

		                    dataIn.close();
		                    dataOut.close();

		                }
		            } catch (IOException ex) {
		                ex.printStackTrace();
		                textArea.append("Error: Unable to receive messages.\n");
		            }
		    }).start();
	}
	
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
		String serverAddress = "127.0.0.1";
		int messagePort = 5001;
		int callPort = 5002;

		/*
		 * Check which button was clicked.
		 */
		if (e.getSource() == sendButton){
			
			sendMessage(serverAddress, messagePort);
			
		}
	}

	/**
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the 
	 * window).
	 */
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		dispose();
        	System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	public void sendMessage(String address, int port) {
		try {
			Socket sendSocket = new Socket(address, port);
			
			PrintWriter out = new PrintWriter(sendSocket.getOutputStream(), true);
			
		    String sendMessage = inputTextField.getText();
		    out.println(sendMessage);
		    textArea.append("Giorgos: " + sendMessage + newline);

		    
		    inputTextField.setText("");
			

	       } catch (IOException e1) {
	           e1.printStackTrace();
	           textArea.append("Error: Unable to send message\n");
	       }
	}
}
	
