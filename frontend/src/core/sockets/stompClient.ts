import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const SOCKET_URL = 'http://localhost:8080/ws';


type PendingSubscription = {
    destination: string;
    callback: (message: IMessage) => void;
};

class StompService {
    private stompClient: Client;
    private pendingSubscriptions: PendingSubscription[] = [];

    constructor() {
        this.stompClient = new Client({
            webSocketFactory: () => new SockJS(SOCKET_URL),
            onConnect: () => {
                console.log('WebSocket Connected');

                this.processPendingSubscriptions();
            },
            onStompError: (frame) => {
                console.error('Broker reported error: ' + frame.headers['message']);
                console.error('Additional details: ' + frame.body);
            },
        });
    }

    private processPendingSubscriptions() {
        this.pendingSubscriptions.forEach(sub => {
            console.log(`Processing pending subscription for: ${sub.destination}`);
            this.stompClient.subscribe(sub.destination, sub.callback);
        });

        this.pendingSubscriptions = [];
    }

    public connect() {
        if (!this.stompClient.active) {
            console.log("Activating STOMP client...");
            this.stompClient.activate();
        }
    }

    public disconnect() {
        if (this.stompClient.active) {
            this.stompClient.deactivate();
            console.log('WebSocket Disconnected');
        }
    }

    public subscribeToSubmissionResult(
        submissionId: string, 
        onResult: (result: any) => void
    ) {
        const destination = `/topic/submission-result/${submissionId}`;
        const callback = (message: IMessage) => {
            onResult(JSON.parse(message.body));
        };

        if (this.stompClient.connected) {
            console.log(`Subscribing immediately to: ${destination}`);
            this.stompClient.subscribe(destination, callback);
        } else {

            console.log(`Connection not ready. Queuing subscription for: ${destination}`);
            this.pendingSubscriptions.push({ destination, callback });
        }
    }
}


export const stompService = new StompService();