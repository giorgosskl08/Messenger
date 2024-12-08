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
	static JButton callButton;	
	static JButton endButton;
	
    /* 
     * Threads for call handling
    */
    private Thread captureThread;
    private Thread receiveThread;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private Socket call_socket;
	
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
		callButton = new JButton("Call");
		endButton = new JButton("End Call");
						
		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);								
		add(inputTextField);
		add(sendButton);
		add(callButton);
		add(endButton);
		
		/*
		 * 3. Linking the buttons to the ActionListener
		 */
		sendButton.addActionListener(this);			
		callButton.addActionListener(this);	
		endButton.addActionListener(this);

		
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
			
		}else if (e.getSource() == callButton) {
			
			makeCall(serverAddress, callPort);
			
		}else if (e.getSource() == endButton) {
			
			endCall();
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
	
	public void makeCall(String address, int port) {
		try {
	        if (call_socket == null || call_socket.isClosed()) {
	            textArea.append("Trying to connect to the server\n");
	            call_socket = new Socket(address, 5001);
	            textArea.append("Connected to the server\n");
	        }

	        InputStream in = call_socket.getInputStream();
	        OutputStream out = call_socket.getOutputStream();

	        AudioFormat audio_format = new AudioFormat(8000, 16, 1, true, true);
	        DataLine.Info audio_info = new DataLine.Info(TargetDataLine.class, audio_format);
	        DataLine.Info source_info = new DataLine.Info(SourceDataLine.class, audio_format);

	        microphone = (TargetDataLine) AudioSystem.getLine(audio_info);
	        microphone.open(audio_format);
	        microphone.start();

	        speakers = (SourceDataLine) AudioSystem.getLine(source_info);
	        speakers.open(audio_format);
	        speakers.start();

	        captureThread = new Thread(() -> {
	            try {
	                byte[] audio_buffer = new byte[1024];
	                while (!Thread.currentThread().isInterrupted()) {
	                    int bytes_read = microphone.read(audio_buffer, 0, audio_buffer.length);
	                    if (bytes_read > 0) {
	                        textArea.append("Captured " + bytes_read + " bytes of audio.\n");
	                    } else {
	                        textArea.append("No audio captured. Possible issue with microphone.\n");
	                    }
	                    out.write(audio_buffer, 0, bytes_read);
	                    textArea.append("Started audio message\n");
	                }
	            } catch (Exception ex) {
	                ex.printStackTrace();
	                textArea.append("Error in captureThread: " + ex.getMessage() + "\n");
	            }
	        });

	        receiveThread = new Thread(() -> {
	            try {
	                textArea.append("Capturing audio\n");
	                byte[] receive_buffer = new byte[1024];
	                int bytesRead;
	                while (!Thread.currentThread().isInterrupted()) {
	                    bytesRead = in.read(receive_buffer);
	                    if (bytesRead > 0) {
	                        textArea.append("Received " + bytesRead + " bytes of audio.\n");
	                        speakers.write(receive_buffer, 0, bytesRead);
	                    }
	                }
	                call_socket.close();
	            } catch (Exception ex) {
	                ex.printStackTrace();
	                textArea.append("Error in receiveThread: " + ex.getMessage() + "\n");
	            }
	        });

	        captureThread.start();
	        receiveThread.start();

	        textArea.append("Call started\n");

	    } catch (LineUnavailableException | IOException ex) {
	        ex.printStackTrace();
	        textArea.append("Error in callButton: " + ex.getMessage() + "\n");
	    }

	}
	
	public void endCall() {
		try {
	        if (captureThread != null && captureThread.isAlive()) {
	            captureThread.interrupt();
	        }
	        if (receiveThread != null && receiveThread.isAlive()) {
	            receiveThread.interrupt();
	        }
	        if (microphone != null) {
	            microphone.stop();
	            microphone.close();
	        }
	        if (speakers != null) {
	            speakers.stop();
	            speakers.close();
	        }
	        if (call_socket != null && !call_socket.isClosed()) {
	            call_socket.close();
	        }
	        textArea.append("Call ended\n");

	    } catch (Exception ex) {
	        ex.printStackTrace();
	        textArea.append("Error in endButton: " + ex.getMessage() + "\n");
	    }
	}

}

