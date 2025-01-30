import {
    IonBadge,
    IonButtons,
    IonCardSubtitle,
    IonChip,
    IonHeader,
    IonListHeader,
    IonMenuButton,
    IonRouterLink,
    IonTitle,
    IonToolbar
} from '@ionic/react';
import { useLocation } from 'react-router';

interface PageHeaderProps {
  title: string;
  base:string;
}

const PageHeader: React.FC<PageHeaderProps> = ({ title,base}) => {
    const {pathname}=useLocation()
    const step=pathname.split("/")[2];
  
return <IonHeader>
        <IonToolbar>
          <IonButtons slot="start">
            <IonMenuButton />
            <IonRouterLink routerLink={"/"+base}>
          <IonTitle>{title}</IonTitle>
          </IonRouterLink>
          {step&&<IonChip color='medium'>{step}</IonChip>}
          </IonButtons>
        </IonToolbar>
      </IonHeader>
}

export default PageHeader;
