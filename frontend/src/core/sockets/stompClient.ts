import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// IMPORTANT: Make sure this URL points to your Spring Boot backend's address
const SOCKET_URL = 'http://localhost:8080/ws';

let stompClient: Client;

/**
 * Connects to the WebSocket server.
 * @param onConnectedCallback A function to be called once the connection is established.
 */
export const connect = (onConnectedCallback: () => void) => {
    // If a client already exists and is active, don't create a new one
    if (stompClient && stompClient.active) {
        console.log('WebSocket is already connected.');
        onConnectedCallback();
        return;
    }
    
    stompClient = new Client({
        webSocketFactory: () => new SockJS(SOCKET_URL),
        onConnect: () => {
            console.log('WebSocket Connected');
            onConnectedCallback();
        },
        onStompError: (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
        },
        // It's good practice to add debug messages
        debug: (str) => {
            console.log(new Date(), str);
        },
    });

    stompClient.activate();
};

/**
 * Disconnects from the WebSocket server.
 */
export const disconnect = () => {
    if (stompClient) {
        stompClient.deactivate();
        console.log('WebSocket Disconnected');
    }
};

/**
 * Subscribes to the result topic for a specific submission.
 * @param submissionId The ID of the submission to listen for.
 * @param onResult A callback function that will receive the submission result.
 */
export const subscribeToSubmissionResult = (
    submissionId: string, 
    onResult: (result: any) => void
) => {
    if (stompClient && stompClient.connected) {
        const destination = `/topic/submission-result/${submissionId}`;
        console.log(`Subscribing to: ${destination}`);
        
        stompClient.subscribe(destination, (message: IMessage) => {
            console.log('Received message:', message.body);
            onResult(JSON.parse(message.body));
        });
    } else {
        console.error('STOMP client is not connected. Cannot subscribe.');
    }
};