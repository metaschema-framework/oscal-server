import React from 'react';
import { IonAccordion, IonItem, IonLabel, IonList } from '@ionic/react';
import { SystemCharacteristics } from '../../types';
import { RenderProps } from './RenderProps';

interface RenderSystemCharacteristicsProps {
  characteristics: SystemCharacteristics;
}

export const RenderSystemCharacteristics: React.FC<RenderSystemCharacteristicsProps> = ({ characteristics }) => {
  if (!characteristics) return null;

  return (
    <IonAccordion value="system-characteristics">
      <IonItem slot="header" color="light">
        <IonLabel>System Characteristics</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <IonList>
          {characteristics['system-name'] && (
            <IonItem>
              <IonLabel>
                <h2>System Name</h2>
                <p>{characteristics['system-name']}</p>
              </IonLabel>
            </IonItem>
          )}
          {characteristics['system-name-short'] && (
            <IonItem>
              <IonLabel>
                <h2>System Short Name</h2>
                <p>{characteristics['system-name-short']}</p>
              </IonLabel>
            </IonItem>
          )}
          {characteristics.description && (
            <IonItem>
              <IonLabel>
                <h2>Description</h2>
                <p>{characteristics.description}</p>
              </IonLabel>
            </IonItem>
          )}
          {characteristics['security-sensitivity-level'] && (
            <IonItem>
              <IonLabel>
                <h2>Security Sensitivity Level</h2>
                <p>{characteristics['security-sensitivity-level']}</p>
              </IonLabel>
            </IonItem>
          )}
          {characteristics.status && (
            <IonItem>
              <IonLabel>
                <h2>Status</h2>
                <p>{characteristics.status.state}</p>
                {characteristics.status.remarks && (
                  <p className="status-remarks">{characteristics.status.remarks}</p>
                )}
              </IonLabel>
            </IonItem>
          )}
          {characteristics.props && <RenderProps props={characteristics.props} />}
        </IonList>
      </div>
    </IonAccordion>
  );
};
