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
	
    // Threads for call handling
    private Thread captureThread;
    private Thread receiveThread;
    private Socket call_socket;
    private TargetDataLine getsound;
    private SourceDataLine hearsound;
	
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
		App app = new App("CN2 - AUTH");  // TODO: You can add the title that will displayed on the Window of the App here																		  
		app.setSize(500,250);				  
		app.setVisible(true);				  

		/*
		 * 2. 
		 */
			new Thread(() -> {
		        	try (ServerSocket serverSocket = new ServerSocket(5001)) {
		                while (true) {
		                    Socket receiveSocket = serverSocket.accept();
		                    BufferedReader reader = new BufferedReader(new InputStreamReader(receiveSocket.getInputStream()));

		                    String receivedMessage;
		                    while ((receivedMessage = reader.readLine()) != null) {
		                        textArea.append("Received: " + receivedMessage + newline);
		                    }

		                    receiveSocket.close();
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

		/*
		 * Check which button was clicked.
		 */
		if (e.getSource() == sendButton){
			
		    int port = 5001;

		    try (Socket send_socket = new Socket(serverAddress, port);
		            OutputStream out = send_socket.getOutputStream();
		            PrintWriter writer = new PrintWriter(out, true)) {

		           String message = inputTextField.getText();
		           textArea.append("Sent: " + message + newline);

		           writer.println(message); // Send the message over the TCP connection

		       } catch (IOException ex) {
		           ex.printStackTrace();
		           textArea.append("Error: Unable to send message\n");
		       }
			
		}else if (e.getSource() == callButton) {
		    
		    int port = 5002;

		    try {
		        if (call_socket == null || call_socket.isClosed()) {
		            textArea.append("Trying to connect to the server\n");
		            call_socket = new Socket(serverAddress, port);
		            textArea.append("Connected to the server\n");
		        }

		        InputStream in = call_socket.getInputStream();
		        OutputStream out = call_socket.getOutputStream();

		        AudioFormat audio_format = new AudioFormat(8000, 16, 1, true, true);
		        DataLine.Info audio_info = new DataLine.Info(TargetDataLine.class, audio_format);
		        DataLine.Info source_info = new DataLine.Info(SourceDataLine.class, audio_format);

		        getsound = (TargetDataLine) AudioSystem.getLine(audio_info);
		        getsound.open(audio_format);
		        getsound.start();

		        hearsound = (SourceDataLine) AudioSystem.getLine(source_info);
		        hearsound.open(audio_format);
		        hearsound.start();

		        captureThread = new Thread(() -> {
		            try {
		                byte[] audio_buffer = new byte[1024];
		                while (!Thread.currentThread().isInterrupted()) {
		                    int bytes_read = getsound.read(audio_buffer, 0, audio_buffer.length);
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
		                        hearsound.write(receive_buffer, 0, bytesRead);
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

		} else if (e.getSource() == endButton) {
		    try {
		        if (captureThread != null && captureThread.isAlive()) {
		            captureThread.interrupt();
		        }
		        if (receiveThread != null && receiveThread.isAlive()) {
		            receiveThread.interrupt();
		        }
		        if (getsound != null) {
		            getsound.stop();
		            getsound.close();
		        }
		        if (hearsound != null) {
		            hearsound.stop();
		            hearsound.close();
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
}
