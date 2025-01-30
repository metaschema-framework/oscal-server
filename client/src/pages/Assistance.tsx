import React from 'react';
import {
  IonContent,
  IonHeader,
  IonPage,
  IonTitle,
  IonToolbar,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonText,
  IonList,
  IonItem,
  IonLabel,
  IonIcon,
} from '@ionic/react';
import { chatbubbleEllipsesOutline, helpCircleOutline, documentTextOutline } from 'ionicons/icons';
import ChatbotWindow from '../components/chatbot/ChatbotWindow';

const Assistance: React.FC = () => {
  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>OSCAL Assistant</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="ion-padding">
        <IonCard>
          <IonCardHeader>
            <IonCardTitle>Welcome to OSCAL Assistant</IonCardTitle>
          </IonCardHeader>
          <IonCardContent>
            <IonText>
              <p>
                Get help with your OSCAL documents and tasks. The AI-powered assistant can help you with:
              </p>
            </IonText>
            <IonList>
              <IonItem>
                <IonIcon icon={documentTextOutline} slot="start" />
                <IonLabel>
                  <h2>Document Management</h2>
                  <p>Create, validate, and modify OSCAL documents</p>
                </IonLabel>
              </IonItem>
              <IonItem>
                <IonIcon icon={helpCircleOutline} slot="start" />
                <IonLabel>
                  <h2>Guidance & Best Practices</h2>
                  <p>Learn OSCAL concepts and implementation strategies</p>
                </IonLabel>
              </IonItem>
              <IonItem>
                <IonIcon icon={chatbubbleEllipsesOutline} slot="start" />
                <IonLabel>
                  <h2>Interactive Support</h2>
                  <p>Get real-time answers to your OSCAL questions</p>
                </IonLabel>
              </IonItem>
            </IonList>
          </IonCardContent>
        </IonCard>

        <IonCard>
          <IonCardHeader>
            <IonCardTitle>Quick Start Guide</IonCardTitle>
          </IonCardHeader>
          <IonCardContent>
            <IonList>
              <IonItem>
                <IonLabel>
                  <h2>1. Ask Questions</h2>
                  <p>Type your OSCAL-related questions in the chat window</p>
                </IonLabel>
              </IonItem>
              <IonItem>
                <IonLabel>
                  <h2>2. Get Document Help</h2>
                  <p>Request assistance with specific OSCAL document types</p>
                </IonLabel>
              </IonItem>
              <IonItem>
                <IonLabel>
                  <h2>3. Validate Documents</h2>
                  <p>Check your OSCAL documents for compliance and correctness</p>
                </IonLabel>
              </IonItem>
            </IonList>
          </IonCardContent>
        </IonCard>

        {/* The ChatbotWindow component will render its own floating button */}
        <ChatbotWindow />
      </IonContent>
    </IonPage>
  );
};

export default Assistance;
