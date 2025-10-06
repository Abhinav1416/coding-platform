// import { Client, type IMessage } from '@stomp/stompjs';
// import SockJS from 'sockjs-client';

// const SOCKET_URL = 'http://localhost:8080/ws';


// type PendingSubscription = {
//     destination: string;
//     callback: (message: IMessage) => void;
// };

// class StompService {
//     private stompClient: Client;
//     private pendingSubscriptions: PendingSubscription[] = [];

//     constructor() {
//         this.stompClient = new Client({
//             webSocketFactory: () => new SockJS(SOCKET_URL),
//             onConnect: () => {
//                 console.log('WebSocket Connected');

//                 this.processPendingSubscriptions();
//             },
//             onStompError: (frame) => {
//                 console.error('Broker reported error: ' + frame.headers['message']);
//                 console.error('Additional details: ' + frame.body);
//             },
//         });
//     }

//     private processPendingSubscriptions() {
//         this.pendingSubscriptions.forEach(sub => {
//             console.log(`Processing pending subscription for: ${sub.destination}`);
//             this.stompClient.subscribe(sub.destination, sub.callback);
//         });

//         this.pendingSubscriptions = [];
//     }

//     public connect() {
//         if (!this.stompClient.active) {
//             console.log("Activating STOMP client...");
//             this.stompClient.activate();
//         }
//     }

//     public disconnect() {
//         if (this.stompClient.active) {
//             this.stompClient.deactivate();
//             console.log('WebSocket Disconnected');
//         }
//     }

//     public subscribeToSubmissionResult(
//         submissionId: string, 
//         onResult: (result: any) => void
//     ) {
//         const destination = `/topic/submission-result/${submissionId}`;
//         const callback = (message: IMessage) => {
//             onResult(JSON.parse(message.body));
//         };

//         if (this.stompClient.connected) {
//             console.log(`Subscribing immediately to: ${destination}`);
//             this.stompClient.subscribe(destination, callback);
//         } else {

//             console.log(`Connection not ready. Queuing subscription for: ${destination}`);
//             this.pendingSubscriptions.push({ destination, callback });
//         }
//     }
// }


// export const stompService = new StompService();


// src/core/sockets/stompClient.ts

// src/core/sockets/stompClient.ts

// import { Client, type IMessage } from '@stomp/stompjs';
// import SockJS from 'sockjs-client';

// const SOCKET_URL = 'http://localhost:8080/ws';

// type PendingSubscription = {
//     destination: string;
//     callback: (message: IMessage) => void;
// };

// class StompService {
//     onConnect(arg0: () => void) {
//         throw new Error('Method not implemented.');
//     }
//     private stompClient: Client;
//     private pendingSubscriptions: PendingSubscription[] = [];

//     constructor() {
//         this.stompClient = new Client({
//             webSocketFactory: () => new SockJS(SOCKET_URL),
//             onConnect: () => {
//                 console.log('WebSocket Connected');
//                 this.processPendingSubscriptions();
//             },
//             onStompError: (frame) => {
//                 console.error('Broker reported error: ' + frame.headers['message']);
//                 console.error('Additional details: ' + frame.body);
//             },
//         });
//     }

//     private processPendingSubscriptions() {
//         this.pendingSubscriptions.forEach(sub => {
//             console.log(`Processing pending subscription for: ${sub.destination}`);
//             this.stompClient.subscribe(sub.destination, sub.callback);
//         });
//         this.pendingSubscriptions = [];
//     }

//     public connect() {
//         if (!this.stompClient.active) {
//             console.log("Activating STOMP client...");
//             this.stompClient.activate();
//         }
//     }

//     public disconnect() {
//         if (this.stompClient.active) {
//             this.stompClient.deactivate();
//             console.log('WebSocket Disconnected');
//         }
//     }

//     public subscribeToSubmissionResult(
//         submissionId: string, 
//         onResult: (result: any) => void
//     ) {
//         const destination = `/topic/submission-result/${submissionId}`;
//         const callback = (message: IMessage) => {
//             onResult(JSON.parse(message.body));
//         };

//         if (this.stompClient.connected) {
//             console.log(`Subscribing immediately to: ${destination}`);
//             this.stompClient.subscribe(destination, callback);
//         } else {
//             console.log(`Connection not ready. Queuing subscription for: ${destination}`);
//             this.pendingSubscriptions.push({ destination, callback });
//         }
//     }
// }

// export const stompService = new StompService();

// src/core/sockets/stompClient.ts

// src/core/sockets/stompClient.ts

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
    
    // --- ADDED: Reference counter to handle React Strict Mode ---
    private connectionCounter = 0;

    constructor() {
        this.stompClient = new Client({
            webSocketFactory: () => new SockJS(SOCKET_URL),
            reconnectDelay: 5000,
            onConnect: () => {
                console.log('STOMP Client Connected');
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

    // --- MODIFIED: connect() now uses the counter ---
    public connect() {
        this.connectionCounter++;
        console.log(`STOMP connect called. Counter is now: ${this.connectionCounter}`);
        // Only activate if this is the first connection request
        if (this.connectionCounter === 1 && !this.stompClient.active && !this.stompClient.connected) {
            console.log("Activating STOMP client...");
            this.stompClient.activate();
        }
    }

    // --- MODIFIED: disconnect() now uses the counter ---
    public disconnect() {
        this.connectionCounter--;
        console.log(`STOMP disconnect called. Counter is now: ${this.connectionCounter}`);
        // Only deactivate if this is the last component disconnecting
        if (this.connectionCounter === 0 && this.stompClient.active) {
            this.stompClient.deactivate().then(() => console.log('STOMP Client Disconnected'));
        }
    }

    public subscribe(
        destination: string,
        callback: (message: IMessage) => void
    ) {
        if (this.stompClient.connected) {
            this.stompClient.subscribe(destination, callback);
        } else {
            this.pendingSubscriptions.push({ destination, callback });
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
        this.subscribe(destination, callback); // Reuse the generic subscribe logic
    }
}

export const stompService = new StompService();