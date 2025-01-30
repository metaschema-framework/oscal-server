import { IonContent, IonPage } from '@ionic/react';
import ControlGroups from '../components/categorize/ControlGroups';
import InformationTypes from '../components/categorize/InformationTypes';
import SystemCategorization from '../components/categorize/SystemCategorization';
import SystemDescription from '../components/categorize/SystemDescription';
import PageHeader from '../components/common/PageHeader';
import SubMenu from '../components/SubMenu';
import OscalForm from '../components/OscalForm';

const Editor: React.FC = () => {

  return (
    <IonPage>
      <PageHeader base='tools' title="Editor" />
      <IonContent>
        <OscalForm type='package'/>
      </IonContent>
    </IonPage>
  );
};

export default Editor;
