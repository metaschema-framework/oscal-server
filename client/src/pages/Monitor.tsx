import React from 'react';
import { IonButtons, IonCard, IonContent, IonHeader, IonMenuButton, IonPage, IonTitle, IonToolbar } from '@ionic/react';
import { useParams } from 'react-router';
import ExploreContainer from '../components/ExploreContainer';
import SubMenu from '../components/SubMenu';
import SystemEnvironmentChanges from '../components/monitor/SystemEnvironmentChanges';
import OngoingAssessment from '../components/monitor/OngoingAssessment';
import StatusReporting from '../components/monitor/StatusReporting';
import OngoingAuthorization from '../components/monitor/OngoingAuthorization';
import PageHeader from '../components/common/PageHeader';

const Monitor: React.FC = () => {
  const { name } = useParams<{ name: string; }>();

  return (
    <IonPage>
      <IonContent fullscreen>
        <PageHeader base='monitor' title='Monitor' />
        <SubMenu routes={[
          { component: SystemEnvironmentChanges, slug: 'system-environment-changes' },
          { component: OngoingAssessment, slug: 'ongoing-assessment' },
          { component: StatusReporting, slug: 'status-reporting' },
          { component: OngoingAuthorization, slug: 'ongoing-authorization' }
        ]}/>
      </IonContent>
    </IonPage>
  );
};

export default Monitor;
