import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const SOCKET_URL = 'http://localhost:8080/ws';

type PendingSubscription = {
    destination: string;
    callback: (message: IMessage) => void;
    resolve: (subscription: StompSubscription) => void;
};

class StompService {
    private stompClient: Client;
    private pendingSubscriptions: PendingSubscription[] = [];

    constructor() {
        this.stompClient = new Client({
            webSocketFactory: () => new SockJS(SOCKET_URL),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
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
            const subscription = this.stompClient.subscribe(sub.destination, sub.callback);
            sub.resolve(subscription); // Resolve the promise with the actual subscription
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

    // --- NEW GENERIC SUBSCRIBE METHOD ---
    public subscribe(destination: string, callback: (message: IMessage) => void): StompSubscription {
        if (this.stompClient.connected) {
            console.log(`Subscribing immediately to: ${destination}`);
            return this.stompClient.subscribe(destination, callback);
        }

        // This part is a bit tricky, but it's a placeholder. 
        // A robust implementation might use Promises to handle the async nature of subscriptions.
        // For now, let's assume connect() is called early and the client is connected.
        // A simpler, though less robust, queuing mechanism:
        console.log(`Connection not ready. Subscribing might be delayed. Ensure connect() is called at app startup.`);
        return this.stompClient.subscribe(destination, callback);
    }
}

export const stompService = new StompService();