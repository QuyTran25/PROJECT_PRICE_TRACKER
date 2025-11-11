package com.pricetracker.server.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * WebSocket Server để push real-time price updates tới frontend clients
 * Port: 8081
 */
public class PriceWebSocketServer extends WebSocketServer {
    
    private static final int PORT = 8081;
    
    // Lưu tất cả connected clients (thread-safe)
    private final Set<WebSocket> clients = Collections.synchronizedSet(new HashSet<>());
    
    public PriceWebSocketServer() {
        super(new InetSocketAddress(PORT));
        setReuseAddr(true); // Cho phép restart nhanh
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        String clientInfo = conn.getRemoteSocketAddress().toString();
        System.out.println("[WebSocket] Client connected: " + clientInfo + " (Total: " + clients.size() + ")");
        
        // Gửi welcome message
        conn.send("{\"type\":\"connected\",\"message\":\"Welcome to Price Tracker WebSocket!\"}");
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        String clientInfo = conn.getRemoteSocketAddress().toString();
        System.out.println("[WebSocket] Client disconnected: " + clientInfo + " (Remaining: " + clients.size() + ")");
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        // Xử lý message từ client (nếu cần)
        System.out.println("[WebSocket] Received from " + conn.getRemoteSocketAddress() + ": " + message);
        
        // Echo back (có thể mở rộng xử lý subscribe/unsubscribe)
        conn.send("{\"type\":\"echo\",\"data\":\"" + message + "\"}");
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[WebSocket] Error: " + ex.getMessage());
        ex.printStackTrace();
        
        if (conn != null) {
            clients.remove(conn);
        }
    }
    
    @Override
    public void onStart() {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║  WebSocket Server Started on Port " + PORT + "   ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        setConnectionLostTimeout(100); // Ping clients every 100 seconds
    }
    
    /**
     * Broadcast message tới TẤT CẢ connected clients
     * @param message JSON string để broadcast
     */
    public void broadcast(String message) {
        synchronized (clients) {
            int successCount = 0;
            int failCount = 0;
            
            for (WebSocket client : clients) {
                try {
                    if (client.isOpen()) {
                        client.send(message);
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    System.err.println("[WebSocket] Failed to send to client: " + e.getMessage());
                    failCount++;
                }
            }
            
            if (successCount > 0) {
                System.out.println("[WebSocket] Broadcasted to " + successCount + " clients" +
                        (failCount > 0 ? " (" + failCount + " failed)" : ""));
            }
        }
    }
    
    /**
     * Lấy số lượng clients đang connect
     */
    public int getClientCount() {
        return clients.size();
    }
    
    /**
     * Shutdown gracefully
     */
    public void shutdown() {
        try {
            System.out.println("[WebSocket] Shutting down... (" + clients.size() + " clients)");
            
            // Gửi disconnect message
            broadcast("{\"type\":\"server_shutdown\",\"message\":\"Server is shutting down\"}");
            
            // Đóng tất cả connections
            synchronized (clients) {
                for (WebSocket client : clients) {
                    try {
                        client.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                clients.clear();
            }
            
            // Stop server
            stop(2000); // Timeout 2 seconds
            System.out.println("[WebSocket] Server stopped successfully");
            
        } catch (Exception e) {
            System.err.println("[WebSocket] Error during shutdown: " + e.getMessage());
        }
    }
}
