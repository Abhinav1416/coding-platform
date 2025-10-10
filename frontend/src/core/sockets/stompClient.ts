import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const SOCKET_URL = 'http://localhost:8080/ws';

// A type for a pending subscription request
interface SubscriptionRequest {
  destination: string;
  callback: (message: IMessage) => void;
  subscription: (sub: StompSubscription) => void; // To store the subscription object later
}

class StompService {
    private stompClient: Client;
    private isConnected = false;
    // A queue to hold subscription requests that are made before connecting
    private subscriptionQueue: SubscriptionRequest[] = [];

    constructor() {
        this.stompClient = new Client({
            webSocketFactory: () => new SockJS(SOCKET_URL),
            reconnectDelay: 5000,
            onConnect: () => {
                console.log('STOMP Client Connected');
                this.isConnected = true;
                // When we connect, process any pending subscriptions in the queue
                this.processSubscriptionQueue();
            },
            onDisconnect: () => {
                console.log('STOMP Client Disconnected');
                this.isConnected = false;
            },
            onStompError: (frame) => {
                console.error('Broker reported error: ' + frame.headers['message']);
                console.error('Additional details: ' + frame.body);
            },
        });
    }

    private processSubscriptionQueue() {
        this.subscriptionQueue.forEach(req => {
            const sub = this.stompClient.subscribe(req.destination, req.callback);
            req.subscription(sub); // Store the actual subscription object
        });
        // Clear the queue after processing
        this.subscriptionQueue = [];
    }
    
    // A single, generic subscribe method
    private subscribe(destination: string, callback: (message: any) => void): StompSubscription {
        // This is a placeholder subscription object. The real one will be populated later.
        let subscription: StompSubscription | null = null;

        const messageCallback = (message: IMessage) => {
            callback(JSON.parse(message.body));
        };

        if (this.isConnected) {
            // If we're already connected, subscribe immediately
            subscription = this.stompClient.subscribe(destination, messageCallback);
        } else {
            // If not connected, add the subscription to the queue to be processed on connection
            this.subscriptionQueue.push({
                destination,
                callback: messageCallback,
                subscription: (sub) => {
                    // When the real subscription is made, update our reference
                    Object.assign(subscriptionRef, sub);
                }
            });
        }

        // We return a reference object that will be updated with the real subscription
        // once the connection is established. This ensures the `unsubscribe` method works.
        const subscriptionRef = {
            unsubscribe: () => {
                subscription?.unsubscribe();
            }
        } as StompSubscription;
        
        return subscriptionRef;
    }

    public connect() {
        if (!this.stompClient.active && !this.stompClient.connected) {
            console.log("Activating STOMP client...");
            this.stompClient.activate();
        }
    }

    public disconnect() {
        if (this.stompClient.active) {
            this.stompClient.deactivate();
        }
    }

    // Your specific subscription methods now use the generic `subscribe` method
    public subscribeToMatchUpdates(matchId: string, onEvent: (event: any) => void): StompSubscription {
        return this.subscribe(`/topic/match/${matchId}`, onEvent);
    }
    
    public subscribeToSubmissionResult(submissionId: string, onResult: (result: any) => void): StompSubscription {
        return this.subscribe(`/topic/submission-result/${submissionId}`, onResult);
    }
    
    public subscribeToCountdown(matchId: string, onEvent: (event: any) => void): StompSubscription {
        return this.subscribe(`/topic/match/${matchId}/countdown`, onEvent);
    }
}

export const stompService = new StompService();