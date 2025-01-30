import { IonButtons, IonCard, IonContent, IonHeader, IonMenuButton, IonPage, IonTitle, IonToolbar } from '@ionic/react';
import { useParams } from 'react-router';
import ExploreContainer from '../components/ExploreContainer';
import SubMenu from '../components/SubMenu';
import ControlImplementation from '../components/implement/ControlImplementation';
import ControlDocumentation from '../components/implement/ControlDocumentation';
import ComponentDefinitions from '../components/implement/ComponentDefinitions';
import PageHeader from '../components/common/PageHeader';

const Implement: React.FC = () => {

  const { name } = useParams<{ name: string; }>();

  return (
    <IonPage>
      <IonContent fullscreen>
        <PageHeader base='implement' title='Implement'/>
        <SubMenu routes={[
          { component: ControlImplementation, slug: 'I1-control-implementation' },
          { component: ControlDocumentation, slug: 'I1-control-documentation' },
          { component: ComponentDefinitions, slug: 'I2-component-definitions' }
        ]}/>
      </IonContent>
    </IonPage>
  );
};

export default Implement;
