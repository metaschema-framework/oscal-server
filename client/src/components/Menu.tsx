import {
  IonContent,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonListHeader,
  IonMenu,
  IonMenuToggle
} from '@ionic/react';
import {
  documentOutline,
  documentSharp
} from 'ionicons/icons';
import { useLocation } from 'react-router-dom';
import { ImplementationState } from '../types';
// Icons imported above

interface AppPage {
  url: string;
  iosIcon: string;
  mdIcon: string;
  title: string;
}

const appPages: AppPage[] = [
  // {
  //   title: 'RMF Wizard',
  //   url: '/rmf',
  //   iosIcon: flower,
  //   mdIcon: flowerSharp
  // },
];
const toolPages: AppPage[] = [
  // {
  //   title: 'Import/Export',
  //   url: '/import-export',
  //   iosIcon: cloudUploadOutline,
  //   mdIcon: cloudUploadSharp
  // },
  {
    title: 'Documents',
    url: '/documents',
    iosIcon: documentOutline,
    mdIcon: documentSharp
  },
  // {
  //   title: 'Assistance',
  //   url: '/assistance',
  //   iosIcon: chatbox,
  //   mdIcon: chatboxSharp
  // },
];

const labels:ImplementationState[] = ["implemented" , "partial" ,"planned" , "alternative" , "not-applicable"];

const Menu: React.FC = () => {
  const location = useLocation();

  return (
    <IonMenu contentId="main" type="overlay">
      <IonContent className="ion-padding">
        <IonList id="inbox-list">
          {appPages.map((appPage, index) => {
            return (
              <IonMenuToggle key={index} autoHide={false}>
                <IonItem className={location.pathname.startsWith(appPage.url) ? 'selected' : ''} routerLink={appPage.url} routerDirection="none" lines="none" detail={false}>
                  <IonIcon aria-hidden="true" slot="start" ios={appPage.iosIcon} md={appPage.mdIcon} />
                  <IonLabel>{appPage.title}</IonLabel>
                </IonItem>
              </IonMenuToggle>
            );
          })}
        </IonList>

        <IonList id="inbox-list">
          <IonListHeader>OSCAL-TOOLS</IonListHeader>
          {toolPages.map((appPage, index) => {
            return (
              <IonMenuToggle key={index} autoHide={false}>
                <IonItem className={location.pathname.startsWith(appPage.url) ? 'selected' : ''} routerLink={appPage.url} routerDirection="none" lines="none" detail={false}>
                  <IonIcon aria-hidden="true" slot="start" ios={appPage.iosIcon} md={appPage.mdIcon} />
                  <IonLabel>{appPage.title}</IonLabel>
                </IonItem>
              </IonMenuToggle>
            );
          })}
        </IonList>
        {/* <IonList id="labels-list">
          <IonListHeader>System Coverage</IonListHeader>
          {labels.map((label, index) => (
            <IonItem lines="none" key={index}>
              <IonIcon aria-hidden="true" slot="start" icon={bookmarkOutline} />
              <IonLabel>{label}</IonLabel>
            </IonItem>
          ))}
        </IonList> */}
      </IonContent>
    </IonMenu>
  );
};

export default Menu;
