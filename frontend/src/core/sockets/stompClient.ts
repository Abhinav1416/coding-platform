import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { MatchEvent } from '../../features/match/types/match';


const SOCKET_URL = 'http://localhost:8080/ws';

// A specific type for the submission result payload for clarity
interface SubmissionResultPayload {
    submissionId: string;
    status: string;
}

type PendingSubscription = {
    destination: string;
    callback: (message: IMessage) => void;
    resolve: (subscription: StompSubscription) => void; // Used for more advanced promise-based subscriptions
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
            sub.resolve(subscription);
        });
        this.pendingSubscriptions = [];
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
            console.log('WebSocket Disconnected');
        }
    }

    // --- REFACTORED AND TYPE-SAFE PUBLIC SUBSCRIBE METHODS ---

    /**
     * Subscribes to updates for a specific match.
     * @returns The StompSubscription object, or undefined if the client is not connected.
     */
    public subscribeToMatchUpdates(
        matchId: string,
        callback: (event: MatchEvent) => void
    ): StompSubscription | undefined {
        const destination = `/topic/match/${matchId}`;
        return this._subscribeWithJsonParsing(destination, callback);
    }

    /**
     * Subscribes to the result of a single code submission.
     * @returns The StompSubscription object, or undefined if the client is not connected.
     */
    public subscribeToSubmissionResult(
        submissionId: string,
        callback: (result: SubmissionResultPayload) => void
    ): StompSubscription | undefined {
        const destination = `/topic/submission-result/${submissionId}`;
        return this._subscribeWithJsonParsing(destination, callback);
    }


    // --- THE CORE GENERIC SUBSCRIBE METHOD (NOW RETURNS A SUBSCRIPTION) ---

    /**
     * The base subscribe method. It is recommended to use the more specific methods above.
     * @returns The StompSubscription object, or undefined if the client is not yet connected.
     */
    public subscribe(
        destination: string,
        callback: (message: IMessage) => void
    ): StompSubscription | undefined {
        if (this.stompClient.connected) {
            return this.stompClient.subscribe(destination, callback);
        } else {
            console.warn(`STOMP client not connected. Subscription to ${destination} was queued. Ensure connect() is called first.`);
            this.pendingSubscriptions.push({ destination, callback, resolve: () => {} });
            return undefined;
        }
    }


    // --- PRIVATE HELPER TO REDUCE CODE DUPLICATION ---

    /**
     * A generic helper that subscribes to a destination and handles JSON parsing.
     */
    private _subscribeWithJsonParsing<T>(
        destination: string,
        callback: (payload: T) => void
    ): StompSubscription | undefined {
        const messageCallback = (message: IMessage) => {
            try {
                const payload: T = JSON.parse(message.body);
                callback(payload);
            } catch (error) {
                console.error(`Failed to parse WebSocket JSON message from ${destination}:`, error);
                console.error("Received body:", message.body);
            }
        };
        // Use the base subscribe method
        return this.subscribe(destination, messageCallback);
    }
}

export const stompService = new StompService();