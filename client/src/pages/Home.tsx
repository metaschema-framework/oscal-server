import { IonContent, IonPage, IonList, IonItem, IonLabel, IonIcon } from '@ionic/react';
import { shieldCheckmarkOutline, codeSlashOutline, documentTextOutline, chatbubbleEllipsesOutline, flower, flowerOutline } from 'ionicons/icons';
import PageHeader from '../components/common/PageHeader';

const Home: React.FC = () => {
  const links:any = [
    { path: '/rmf', label: 'Risk Management Walkthrough', icon: shieldCheckmarkOutline,disabled:true },
    { path: '/editor', label: 'Oscal Editor', icon: codeSlashOutline },
    { path: '/documents', label: 'Document manager', icon: documentTextOutline },
    { path: '/assistance', label: 'Oscal Assistant', icon: chatbubbleEllipsesOutline },
    { path: '/import-export', label: 'Import Oscal', icon: flowerOutline },
  ];

  return (
    <IonPage>
      <PageHeader base='/' title="Home" />
      <IonContent>
        <IonList>
          {links.map((link:any) => (
            <IonItem disabled={link?.disabled} key={link.path} routerLink={link.path}>
              <IonIcon slot="start" icon={link.icon} color="primary" style={{ fontSize: '24px' }} />
              <IonLabel color="primary">
                {link.label}
              </IonLabel>
            </IonItem>
          ))}
        </IonList>
      </IonContent>
    </IonPage>
  );
};

export default Home;
