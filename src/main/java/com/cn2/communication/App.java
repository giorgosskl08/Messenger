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
    private DatagramSocket call_socket;

	
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
	public static void main(String[] args) {
	
		/*
		 * 1. Create the app's window
		 */
		App app = new App("Chat and Call over UDP");
		app.setSize(500,250);				  
		app.setVisible(true);				  

		/*
		 * 2. Receive messages constantly in the main method
		 */
			new Thread(() -> {
		            try {
		                // Create a DatagramSocket to receive the data
		                DatagramSocket receive_socket = new DatagramSocket(5002);

		                // Buffer to hold incoming data
		                byte[] buffer = new byte[1024];

		                while (true) {
		                	
		                    // Create a DatagramPacket to receive data
		                    DatagramPacket receive_packet = new DatagramPacket(buffer, buffer.length);
		                    
		                    // Receive the packet
		                    receive_socket.receive(receive_packet);
		                    
		                    // Extract the message from the packet
		                    String receivedMessage = new String(receive_packet.getData(), 0, receive_packet.getLength());

		                    // Display the received message in the textArea
		                    textArea.append("Anatoli: " + receivedMessage + newline);
		                 
		                }
		            } catch (Exception ex) {
		                ex.printStackTrace();
		                textArea.append("Cannot receive messages");
		            }
		    }).start();
	}
	
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
		String ip_address = "127.0.0.1";
		int messagePort = 5002;
		int callPort = 5001;

		/*
		 * Check which button was clicked.
		 */
		if (e.getSource() == sendButton){
			
			// The "Send" button was clicked
			
			sendMessage(ip_address, messagePort);
		
			
		}else if(e.getSource() == callButton){
			
			// The "Call" button was clicked			
			
			makeCall(ip_address, callPort);
			
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
            // Create a DatagramSocket to send the data
            DatagramSocket send_socket = new DatagramSocket();

            // Get the message from the inputTextField
            String message = inputTextField.getText();

            // Convert the message to bytes
            byte[] buffer = message.getBytes();

            // Create a DatagramPacket with the message, IP, and a port number
            InetAddress local_address = InetAddress.getByName(address);
            DatagramPacket send_packet = new DatagramPacket(buffer, buffer.length, local_address, port);

            // Send the packet through the socket
            send_socket.send(send_packet);

            // Display the sent message in the textArea
            textArea.append("Giorgos: " + message + newline);
            
            // Clear the inputTextField
            inputTextField.setText("");

            // Close the socket
            send_socket.close();
            
        } catch (Exception ex) {
            ex.printStackTrace();
            textArea.append("Cannot send message");
        }
	}
	
	public void makeCall(String address, int port) {
		try {
			// Check if the call socket is closed, and if so, initialize it
			if (call_socket == null || call_socket.isClosed()) {
			    call_socket = new DatagramSocket(); // Create a new DatagramSocket for sending audio packets
			}

			// Create the audio format for the audio communication
			AudioFormat audio_format = new AudioFormat(8000, 16, 1, true, true); 
			// 8000 Hz sampling rate, 16-bit samples, 1 channel (mono), signed, big-endian

			// Create DataLine.Info objects for the target (input) and source (output) audio lines
			DataLine.Info audio_info = new DataLine.Info(TargetDataLine.class, audio_format);
			DataLine.Info source_info = new DataLine.Info(SourceDataLine.class, audio_format);

			// Get and configure the TargetDataLine for capturing audio from the microphone
			microphone = (TargetDataLine) AudioSystem.getLine(audio_info);
			microphone.open(audio_format); // Open the audio line with the specified format
			microphone.start();

			// Get and configure the SourceDataLine for playing received audio to the speaker
			speakers = (SourceDataLine) AudioSystem.getLine(source_info);
			speakers.open(audio_format); // Open the audio line with the specified format
			speakers.start(); // Start playback

			// Thread for capturing and sending audio data
			captureThread = new Thread(() -> {
			    try {
			        byte[] audio_buffer = new byte[1024]; // Buffer to store audio data
			        InetAddress call_address = InetAddress.getByName(address); // Localhost IP for testing

			        while (!Thread.currentThread().isInterrupted()) {
			            // Read audio data from the microphone into the buffer
			            int bytes_read = microphone.read(audio_buffer, 0, audio_buffer.length);
			            // Create a packet containing the audio data and send it via the socket
			            DatagramPacket call_packet = new DatagramPacket(audio_buffer, bytes_read, call_address, port);
			            call_socket.send(call_packet);
			        }
			    } catch (Exception ex) {
			        ex.printStackTrace();
			    }
			});

			// Thread for receiving and playing audio data
			receiveThread = new Thread(() -> {
			    try {
			        DatagramSocket receive_socket = new DatagramSocket(port);
			        byte[] receive_buffer = new byte[1024];
			        DatagramPacket receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);

			        while (!Thread.currentThread().isInterrupted()) {
			            // Receive audio data packets
			            receive_socket.receive(receive_packet);
			            // Play the received audio data through the speaker
			            speakers.write(receive_packet.getData(), 0, receive_packet.getLength());
			        }
			        receive_socket.close(); // Close the socket when the thread is interrupted
			    } catch (Exception ex) {
			        ex.printStackTrace();
			        textArea.append("Cannot receive call" + newline);
			    }
			});

			// Start the threads for capturing and receiving audio
			captureThread.start();
			receiveThread.start();

			textArea.append("Call started" + newline); // Log the call start in the user interface

			} catch (LineUnavailableException | IOException ex) {
			    ex.printStackTrace(); // Handle exceptions related to audio line or socket initialization
			}
	}
	
	public void endCall() {
		try {
	        // Stop the capture thread if it is running
	        if (captureThread != null && captureThread.isAlive()) {
	            captureThread.interrupt();
	        }
	        // Stop the receive thread if it is running
	        if (receiveThread != null && receiveThread.isAlive()) {
	            receiveThread.interrupt();
	        }
	        // Stop and close the audio capture line
	        if (microphone != null) {
	            microphone.stop();
	            microphone.close(); 
	        }
	        // Stop and close the audio playback line
	        if (speakers != null) {
	            speakers.stop();
	            speakers.close(); 
	        }
	        // Close the call socket if it is open
	        if (call_socket != null && !call_socket.isClosed()) {
	            call_socket.close();
	        }
	        textArea.append("Call ended" + newline); // Log the call end in the user interface

	    } catch (Exception ex) {
	        ex.printStackTrace(); // Handle any exceptions during resource cleanup
	    }
	}
}
