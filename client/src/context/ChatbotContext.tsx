import React, { createContext, useState, useContext, ReactNode } from 'react';

interface ChatMessage {
  role: 'user' | 'bot' | 'system';
  text: string;
  timestamp?: number;
}

interface ChatbotContextValue {
  messages: ChatMessage[];
  addMessage: (role: 'user' | 'bot' | 'system', text: string) => void;
  clearMessages: () => void;
}

const ChatbotContext = createContext<ChatbotContextValue | undefined>(undefined);

export function ChatbotProvider({ children }: { children: ReactNode }) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);

  const addMessage = (role: 'user' | 'bot' | 'system', text: string) => {
    setMessages((prev) => [...prev, { 
      role, 
      text,
      timestamp: Date.now()
    }]);
  };

  const clearMessages = () => {
    setMessages([]);
  };

  return (
    <ChatbotContext.Provider value={{ messages, addMessage, clearMessages }}>
      {children}
    </ChatbotContext.Provider>
  );
}

export function useChatbot() {
  const context = useContext(ChatbotContext);
  if (!context) {
    throw new Error('useChatbot must be used within a ChatbotProvider');
  }
  return context;
}

export default ChatbotContext;
