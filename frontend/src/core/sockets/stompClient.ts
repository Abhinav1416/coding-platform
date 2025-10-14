import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const SOCKET_URL = import.meta.env.VITE_WEBSOCKET_URL;


interface SubscriptionRequest {
  destination: string;
  callback: (message: IMessage) => void;
  subscription: (sub: StompSubscription) => void;
}

class StompService {
    private stompClient: Client;
    private isConnected = false;
    private subscriptionQueue: SubscriptionRequest[] = [];

    constructor() {
        this.stompClient = new Client({
            webSocketFactory: () => new SockJS(SOCKET_URL),
            reconnectDelay: 5000,
            onConnect: () => {
                console.log('STOMP Client Connected');
                this.isConnected = true;
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
            req.subscription(sub);
        });
        this.subscriptionQueue = [];
    }
    
    private subscribe(destination: string, callback: (message: any) => void): StompSubscription {
        let subscription: StompSubscription | null = null;

        const messageCallback = (message: IMessage) => {
            callback(JSON.parse(message.body));
        };

        if (this.isConnected) {
            subscription = this.stompClient.subscribe(destination, messageCallback);
        } else {
            this.subscriptionQueue.push({
                destination,
                callback: messageCallback,
                subscription: (sub) => {
                    Object.assign(subscriptionRef, sub);
                }
            });
        }

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