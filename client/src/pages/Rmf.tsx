import { IonContent, IonPage, IonList, IonItem, IonLabel } from '@ionic/react';
import PageHeader from '../components/common/PageHeader';
import StepLink from '../components/StepLink';

const Rmf: React.FC = () => {
  const links = [
    { path: '/prepare', label: 'Prepare' },
    { path: '/select', label: 'Select' },
    { path: '/categorize', label: 'Categorize' },
    { path: '/implement', label: 'Implement' },
    { path: '/assess', label: 'Assess' },
    { path: '/authorize', label: 'Authorize' },
    { path: '/monitor', label: 'Monitor' },
  ];

  return (
    <IonPage>
      <PageHeader base='/' title="Risk Management Framework" />
      <IonContent>
        <IonList>
          {links.map((link) => (
            <StepLink base={link.path} label={link.label}/>
          ))}
        </IonList>
      </IonContent>
    </IonPage>
  );
};

export default Rmf;