import { IonButton, IonContent, IonPage } from '@ionic/react';
import PageHeader from '../components/common/PageHeader';
import StepLink from '../components/StepLink';

import React, { useState, useEffect } from 'react';

const Prepare: React.FC = () => {
  return (
    <IonPage>
      <PageHeader base='prepare' title="Prepare Phase" />
      <IonContent>
      <StepLink base='/organization-prep/' label='Prepare Organization'/>
      <StepLink base='/system-prep' label='Prepare System'/>
      {/* <SchemaFlattenerComponent/> */}
      </IonContent>
    </IonPage>
  );
};

export default Prepare;
