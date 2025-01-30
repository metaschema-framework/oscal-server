import { IonContent, IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem } from '@ionic/react';

const MonitoringStrategy: React.FC = () => {
  return (
    <IonContent>
      <IonCard>
        <IonCardHeader>
          <IonCardTitle>S-5 Monitoring Strategy</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          <IonList>
            <IonItem>
              Continuous monitoring strategy is developed for the system and its environment of operation.
            </IonItem>
            <IonItem>
              Strategy includes metrics, frequencies of monitoring activities, and actions to be taken in response to monitoring results.
            </IonItem>
            <IonItem>
              Monitoring processes are integrated with organizational continuous monitoring strategy.
            </IonItem>
          </IonList>
          [Cybersecurity Framework: Profile; DE.CM, RS.MI]
        </IonCardContent>
      </IonCard>
    </IonContent>
  );
};

export default MonitoringStrategy;
