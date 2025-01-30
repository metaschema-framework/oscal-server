import React from 'react';
import {
  IonContent,
  IonItem,
  IonLabel,
  IonButton,
  IonList,
  IonIcon,
  IonGrid,
  IonRow,
  IonCol,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent,
  IonText,
  IonChip,
} from '@ionic/react';
import { trash } from 'ionicons/icons';
import { useOscal } from '../../context/OscalContext';
import OscalForm from '../OscalForm';

const ContinuousMonitoringStrategy: React.FC = () => {
  const { insert, all, destroy } = useOscal();
  const ssp = all('system-security-plan') || {};
  const strategies = Object.values(ssp).map(plan => plan['monitoring-strategy']).filter(Boolean);

  const handleSubmit = async (strategy: any) => {
    // Create or update the SSP with the monitoring strategy
    const existingSSP = Object.values(ssp)[0] || {
      uuid: crypto.randomUUID(),
      'monitoring-strategy': {}
    };

    await insert('system-security-plan', {
      ...existingSSP,
      'monitoring-strategy': strategy
    });
  };

  const renderStrategyDetails = (strategy: any) => (
    <IonItem key={strategy.uuid}>
      <IonLabel className="ion-text-wrap">
        {strategy.title && (
          <h2 className="ion-padding-bottom">{strategy.title}</h2>
        )}
        {strategy.description && (
          <p>{strategy.description}</p>
        )}
        {strategy.metrics?.map((metric: any, index: number) => (
          <IonChip key={index}>
            {metric.name}: {metric.frequency}
          </IonChip>
        ))}
        {strategy.procedures?.map((procedure: any, index: number) => (
          <div key={index} className="ion-padding-vertical">
            <h3>{procedure.title}</h3>
            <p>{procedure.description}</p>
          </div>
        ))}
        {strategy.remarks && (
          <p className="ion-padding-top ion-text-small">
            <IonText color="medium">{strategy.remarks}</IonText>
          </p>
        )}
      </IonLabel>
    </IonItem>
  );

  return (
    <IonContent>
      <IonGrid>
        <IonRow>
          <IonCol size="4">
            <IonCard>
              <IonCardHeader>
                <IonCardTitle>Monitoring Strategy</IonCardTitle>
              </IonCardHeader>
              <IonCardContent>
                <IonList>
                  {strategies.length > 0 ? (
                    strategies.map(renderStrategyDetails)
                  ) : (
                    <IonItem>
                      <IonLabel>
                        <IonText color="medium">
                          No monitoring strategy defined yet.
                        </IonText>
                      </IonLabel>
                    </IonItem>
                  )}
                </IonList>
              </IonCardContent>
            </IonCard>
          </IonCol>
          <IonCol size="8">
            <OscalForm 
              type="system-security-plan" 
              onSubmit={handleSubmit}
            />
          </IonCol>
        </IonRow>
      </IonGrid>
    </IonContent>
  );
};

export default ContinuousMonitoringStrategy;
