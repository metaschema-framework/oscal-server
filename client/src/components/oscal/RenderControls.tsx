import React from 'react';
import { IonAccordion, IonItem, IonLabel, IonList } from '@ionic/react';
import { Control, Property } from '../../types';
import { RenderProps } from './RenderProps';

interface RenderControlsProps {
  controls: (Control | {
    id: string;
    title: string;
    props?: Property[];
  })[];
  isNested?: boolean;
}

export const RenderControls: React.FC<RenderControlsProps> = ({ controls, isNested = false }) => {
  if (!controls?.length) return null;
  
  const content = (
    <IonList>
      {controls.map((control, index) => (
        <IonItem key={control.id || index}>
          <IonLabel>
            <h2>{control.title}</h2>
            <p>ID: {control.id}</p>
            {control.props && <RenderProps props={control.props} />}
          </IonLabel>
        </IonItem>
      ))}
    </IonList>
  );

  if (isNested) {
    return (
      <div className="nested-content ion-padding">
        <div className="nested-header">
          <h3>Controls</h3>
        </div>
        {content}
      </div>
    );
  }

  return (
    <IonAccordion value="controls">
      <IonItem slot="header" color="light">
        <IonLabel>Controls</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        {content}
      </div>
    </IonAccordion>
  );
};
