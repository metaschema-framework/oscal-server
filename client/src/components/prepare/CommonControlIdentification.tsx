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
  IonBadge,
  IonChip,
} from '@ionic/react';
import { trash } from 'ionicons/icons';
import { useOscal } from '../../context/OscalContext';
import OscalForm from '../OscalForm';

const getImplementationStatus = (control: any) => {
  const status = control['by-components']?.[0]?.['implementation-status']?.state;
  switch (status) {
    case 'implemented': return { text: 'Implemented', color: 'success' };
    case 'partial': return { text: 'Partial', color: 'warning' };
    case 'planned': return { text: 'Planned', color: 'primary' };
    case 'alternative': return { text: 'Alternative', color: 'tertiary' };
    case 'not-applicable': return { text: 'N/A', color: 'medium' };
    default: return { text: 'Unknown', color: 'medium' };
  }
};

const CommonControlIdentification: React.FC = () => {
  const { insert, all, destroy } = useOscal();
  const controls = all('common-control') || {};

  const handleDelete = async (uuid: string) => {
    await destroy('common-control', uuid);
  };

  const renderControlDetails = (control: any) => {
    const status = getImplementationStatus(control);
    return (
      <IonItem key={control.uuid}>
        <IonLabel className="ion-text-wrap">
          <div className="ion-padding-vertical">
            <h2>
              {control['control-id']}
              <IonBadge color={status.color} className="ion-margin-start">
                {status.text}
              </IonBadge>
            </h2>
          </div>
          {control.props?.map((prop: any, index: number) => (
            <IonChip key={index}>
              {prop.name}: {prop.value}
            </IonChip>
          ))}
          {control['responsible-roles']?.map((role: any, index: number) => (
            <div key={index} className="ion-padding-vertical">
              <IonText color="medium">Role: {role['role-id']}</IonText>
            </div>
          ))}
          {control['by-components']?.[0]?.description && (
            <p className="ion-padding-top">
              {control['by-components'][0].description}
            </p>
          )}
          {control.remarks && (
            <p className="ion-padding-top ion-text-small">
              <IonText color="medium">{control.remarks}</IonText>
            </p>
          )}
        </IonLabel>
        <IonButton 
          fill="clear" 
          slot="end"
          onClick={() => handleDelete(control.uuid)}
        >
          <IonIcon icon={trash} />
        </IonButton>
      </IonItem>
    );
  };

  return (
    <IonContent>
      <IonGrid>
        <IonRow>
          <IonCol size="4">
            <IonCard>
              <IonCardHeader>
                <IonCardTitle>Common Controls</IonCardTitle>
              </IonCardHeader>
              <IonCardContent>
                <IonList>
                  {Object.values(controls).length > 0 ? (
                    Object.values(controls).map(renderControlDetails)
                  ) : (
                    <IonItem>
                      <IonLabel>
                        <IonText color="medium">
                          No common controls defined yet.
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
              type="common-control" 
              onSubmit={(control) => insert('common-control', control)}
            />
          </IonCol>
        </IonRow>
      </IonGrid>
    </IonContent>
  );
};

export default CommonControlIdentification;
