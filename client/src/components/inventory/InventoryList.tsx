import React from 'react';
import {
  IonContent,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonList,
  IonItem,
  IonLabel,
  IonGrid,
  IonRow,
  IonCol,
  IonButton,
  IonIcon,
} from '@ionic/react';
import { useOscal } from '../../context/OscalContext';
import OscalForm from '../OscalForm';
import Search from '../common/Search';
import { trash } from 'ionicons/icons';

const InventoryList: React.FC = () => {
  const { insert, all, destroy } = useOscal();
  const items = all('inventory-item') || {};

  const handleDelete = async (uuid: string) => {
    await destroy('inventory-item', uuid);
  };

  return (
    <IonContent>
      <IonGrid>
        <IonRow>
          <IonCol size="4">
            <IonCard>
              <IonCardHeader>
                <IonCardTitle>Inventory Items</IonCardTitle>
              </IonCardHeader>
              <IonCardContent>
                <Search context="inventory" />
                <IonList>
                  {Object.values(items).map((item: any) => (
                    <IonItem key={item.uuid}>
                      <IonLabel>
                        <h2>{item.description}</h2>
                        {item.props?.map((prop: any) => (
                          <p key={prop.name}>
                            {prop.name}: {prop.value}
                          </p>
                        ))}
                      </IonLabel>
                      <IonButton 
                        fill="clear" 
                        slot="end"
                        onClick={() => handleDelete(item.uuid)}
                      >
                        <IonIcon icon={trash} />
                      </IonButton>
                    </IonItem>
                  ))}
                </IonList>
              </IonCardContent>
            </IonCard>
          </IonCol>
          <IonCol size="8">
            <OscalForm 
              type="inventory-item" 
              onSubmit={(item) => insert('inventory-item', item)}
            />
          </IonCol>
        </IonRow>
      </IonGrid>
    </IonContent>
  );
};

export default InventoryList;
