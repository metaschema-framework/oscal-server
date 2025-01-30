import React from 'react';
import { IonButton, IonIcon, IonContent } from '@ionic/react';
import { star, add, trash, cloudUpload, heart, arrowForward } from 'ionicons/icons';

const IonButtonTemplates: React.FC = () => {
  return (
    <IonContent>
      <div style={{ padding: '20px' }}>
        {/* Fill Variations */}
        <h3>Fill Variations</h3>
        <IonButton>Default</IonButton>
        <IonButton fill="clear">Clear</IonButton>
        <IonButton fill="outline">Outline</IonButton>
        <IonButton fill="solid">Solid</IonButton>

        {/* Color Variations */}
        <h3>Color Variations</h3>
        <IonButton color="primary">Primary</IonButton>
        <IonButton color="secondary">Secondary</IonButton>
        <IonButton color="tertiary">Tertiary</IonButton>
        <IonButton color="success">Success</IonButton>
        <IonButton color="warning">Warning</IonButton>
        <IonButton color="danger">Danger</IonButton>
        <IonButton color="light">Light</IonButton>
        <IonButton color="medium">Medium</IonButton>
        <IonButton color="dark">Dark</IonButton>

        {/* Size Variations */}
        <h3>Size Variations</h3>
        <IonButton size="small">Small</IonButton>
        <IonButton size="default">Default</IonButton>
        <IonButton size="large">Large</IonButton>

        {/* Expand Variations */}
        <h3>Expand Variations</h3>
        <IonButton expand="block">Block</IonButton>
        <IonButton expand="full">Full</IonButton>

        {/* Icons */}
        <h3>Icons</h3>
        <IonButton>
          <IonIcon slot="start" icon={star}></IonIcon>
          Start Icon
        </IonButton>
        <IonButton>
          End Icon
          <IonIcon slot="end" icon={arrowForward}></IonIcon>
        </IonButton>
        <IonButton>
          <IonIcon slot="icon-only" icon={add}></IonIcon>
        </IonButton>

        {/* Combined Examples */}
        <h3>Combined Examples</h3>
        <IonButton color="success" fill="outline" size="small">
          <IonIcon slot="start" icon={cloudUpload}></IonIcon>
          Upload
        </IonButton>
        <IonButton color="danger" fill="solid" size="large">
          <IonIcon slot="start" icon={trash}></IonIcon>
          Delete
        </IonButton>
        <IonButton color="tertiary" fill="clear">
          <IonIcon slot="end" icon={heart}></IonIcon>
          Like
        </IonButton>

        {/* Disabled State */}
        <h3>Disabled State</h3>
        <IonButton disabled>Disabled Button</IonButton>
        <IonButton disabled fill="outline">Disabled Outline</IonButton>

        {/* Strong Buttons */}
        <h3>Strong Buttons</h3>
        <IonButton strong={true}>Strong Button</IonButton>

        {/* Shape Variations */}
        <h3>Shape Variations</h3>
        <IonButton shape="round">Round Button</IonButton>
      </div>
    </IonContent>
  );
};

export default IonButtonTemplates;
