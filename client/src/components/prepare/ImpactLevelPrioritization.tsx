import {
  IonPage,
  IonContent,
  IonItem,
  IonLabel,
  IonSelect,
  IonSelectOption,
  IonButton,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent,
} from '@ionic/react';
import React, { useState } from 'react';
import { useOscal } from '../../context/OscalContext';
import { SecurityImpactLevel } from '../../types';

const ImpactLevelPrioritization: React.FC = () => {
  const { all, read ,insert} = useOscal();
  const [levels, setLevels] = useState<SecurityImpactLevel>({"security-objective-availability":'',"security-objective-confidentiality":'',"security-objective-integrity":''});

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    insert('security-impact-level',levels)
  };

  return (
    <IonPage>
      <IonContent>
        <IonCard>
          <IonCardHeader>
            <IonCardTitle>Security Impact Level Prioritization</IonCardTitle>
          </IonCardHeader>
          <IonCardContent>
            <form onSubmit={handleSubmit}>
              <IonItem>
                <IonLabel position="stacked">Confidentiality Impact</IonLabel>
                <IonSelect
                  value={levels['security-objective-confidentiality']}
                  onIonChange={e => setLevels({
                    ...levels,
                    'security-objective-confidentiality': e.detail.value
                  })}
                >
                  <IonSelectOption value="low">Low</IonSelectOption>
                  <IonSelectOption value="moderate">Moderate</IonSelectOption>
                  <IonSelectOption value="high">High</IonSelectOption>
                </IonSelect>
              </IonItem>

              <IonItem>
                <IonLabel position="stacked">Integrity Impact</IonLabel>
                <IonSelect
                  value={levels['security-objective-integrity']}
                  onIonChange={e => setLevels({
                    ...levels,
                    'security-objective-integrity': e.detail.value
                  })}
                >
                  <IonSelectOption value="low">Low</IonSelectOption>
                  <IonSelectOption value="moderate">Moderate</IonSelectOption>
                  <IonSelectOption value="high">High</IonSelectOption>
                </IonSelect>
              </IonItem>

              <IonItem>
                <IonLabel position="stacked">Availability Impact</IonLabel>
                <IonSelect
                  value={levels['security-objective-availability']}
                  onIonChange={e => setLevels({
                    ...levels,
                    'security-objective-availability': e.detail.value
                  })}
                >
                  <IonSelectOption value="low">Low</IonSelectOption>
                  <IonSelectOption value="moderate">Moderate</IonSelectOption>
                  <IonSelectOption value="high">High</IonSelectOption>
                </IonSelect>
              </IonItem>

              <IonButton expand="block" type="submit" className="ion-margin-top">
                Save Impact Levels
              </IonButton>
            </form>
          </IonCardContent>
        </IonCard>
      </IonContent>
    </IonPage>
  );
};

export default ImpactLevelPrioritization;
