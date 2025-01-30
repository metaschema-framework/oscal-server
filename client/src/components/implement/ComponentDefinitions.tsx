import React from 'react';
import {
  IonContent,
  IonHeader,
  IonPage,
  IonTitle,
  IonToolbar,
  IonList,
  IonItem,
  IonLabel,
  IonGrid,
  IonRow,
  IonCol,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent,
  IonText,
  IonChip,
  IonButton,
  IonIcon,
} from '@ionic/react';
import { useOscal } from '../../context/OscalContext';
import OscalForm from '../OscalForm';
import { trash } from 'ionicons/icons';
import { Property, ServiceProtocolInformation } from '../../types';

const ComponentDefinitions: React.FC = () => {
  const { insert, all, destroy } = useOscal();
  const components = all('system-component') || {};

  const handleDelete = async (uuid: string) => {
    await destroy('system-component', uuid);
  };

  const renderComponentDetails = (component: any) => (
    <IonItem key={component.uuid}>
      <IonLabel>
        <h2>{component.title}</h2>
        <p><strong>Type:</strong> {component.type}</p>
        <p>{component.description}</p>
        {component.props && (
          <div className="ion-padding-top">
            {component.props.map((prop: Property, index: number) => (
              <IonChip key={index}>
                {prop.name}: {prop.value}
              </IonChip>
            ))}
          </div>
        )}
        {component.protocols && (
          <div className="ion-padding-top">
            {component.protocols.map((protocol: ServiceProtocolInformation, index: number) => (
              <IonChip key={index}>
                {protocol.name}
              </IonChip>
            ))}
          </div>
        )}
      </IonLabel>
      <IonButton 
        fill="clear" 
        slot="end"
        onClick={() => handleDelete(component.uuid)}
      >
        <IonIcon icon={trash} />
      </IonButton>
    </IonItem>
  );

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Component Definitions</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <IonGrid>
          <IonRow>
            <IonCol size="4">
              <IonCard>
                <IonCardHeader>
                  <IonCardTitle>Defined Components</IonCardTitle>
                </IonCardHeader>
                <IonCardContent>
                  <IonList>
                    {Object.values(components).length > 0 ? (
                      Object.values(components).map(renderComponentDetails)
                    ) : (
                      <IonItem>
                        <IonLabel>
                          <IonText color="medium">
                            No components defined yet.
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
                type="system-component" 
                onSubmit={(component) => insert('system-component', component)}
              />
            </IonCol>
          </IonRow>
        </IonGrid>
      </IonContent>
    </IonPage>
  );
};

export default ComponentDefinitions;
