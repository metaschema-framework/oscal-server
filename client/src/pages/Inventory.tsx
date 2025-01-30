import React from 'react';
import {
  IonContent,
  IonHeader,
  IonPage,
  IonTitle,
  IonToolbar,
} from '@ionic/react';
import InventoryList from '../components/inventory/InventoryList';
import Search from '../components/common/Search';

const Inventory: React.FC = () => {
  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>System Inventory</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <Search context="inventory" />
        <InventoryList />
      </IonContent>
    </IonPage>
  );
};

export default Inventory;
