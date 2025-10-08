import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { MatchEvent } from '../../features/match/types/match';

const SOCKET_URL = 'http://localhost:8080/ws';

class StompService {
    private stompClient: Client;

    constructor() {
        this.stompClient = new Client({
            webSocketFactory: () => new SockJS(SOCKET_URL),
            reconnectDelay: 5000,
            onConnect: () => {
                console.log('STOMP Client Connected');
            },
            onStompError: (frame) => {
                console.error('Broker reported error: ' + frame.headers['message']);
                console.error('Additional details: ' + frame.body);
            },
        });
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
            console.log('STOMP Client Disconnected');
        }
    }

    // ✅ NEW: A specific, type-safe method for match events
    public subscribeToMatchUpdates(matchId: string, onEvent: (event: MatchEvent) => void): StompSubscription {
        const destination = `/topic/match/${matchId}`;
        return this.stompClient.subscribe(destination, (message: IMessage) => {
            const event: MatchEvent = JSON.parse(message.body);
            onEvent(event);
        });
    }

    // ✅ NEW: A specific method for submission results
    public subscribeToSubmissionResult(submissionId: string, onResult: (result: any) => void): StompSubscription {
        const destination = `/topic/submission-result/${submissionId}`;
        return this.stompClient.subscribe(destination, (message: IMessage) => {
            const result = JSON.parse(message.body);
            onResult(result);
        });
    }
}

export const stompService = new StompService();