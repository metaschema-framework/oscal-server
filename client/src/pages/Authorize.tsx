import React from 'react';
import { IonContent, IonPage } from '@ionic/react';
import PageHeader from '../components/common/PageHeader';
import ExploreContainer from '../components/ExploreContainer';
import SubMenu from '../components/SubMenu';
import AuthorizationPackage from '../components/authorize/AuthorizationPackage';
import RiskDetermination from '../components/authorize/RiskDetermination';

const Authorize: React.FC = () => {
  return (
    <IonPage>
      <PageHeader base='authorize' title="Authorize Phase" />
      <IonContent fullscreen>
        <SubMenu routes={[
          { component: AuthorizationPackage, slug: 'authorization-package' },
          { component: RiskDetermination, slug: 'risk-determination' }
        ]}/>
      </IonContent>
    </IonPage>
  );
};

export default Authorize;
