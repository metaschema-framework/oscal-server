import React from 'react';
import { IonAccordion, IonItem, IonLabel } from '@ionic/react';
import { MergeControls } from '../../types';
import { RenderGroups } from './RenderGroups';

interface RenderMergeProps {
  merge: MergeControls;
}

export const RenderMerge: React.FC<RenderMergeProps> = ({ merge }) => {
  return (
    <IonAccordion value="merge">
      <IonItem slot="header" color="light">
        <IonLabel>Merge Controls</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        {merge.combine && (
          <div>
            <h4>Combination Method</h4>
            <p>{merge.combine.method}</p>
          </div>
        )}
        {merge.flat && <p>Controls appear without grouping structure</p>}
        {merge['as-is'] && <p>Controls retain original grouping</p>}
        {merge.custom && (
          <div>
            <h4>Custom Grouping</h4>
            {merge.custom.groups && <RenderGroups groups={merge.custom.groups} />}
          </div>
        )}
      </div>
    </IonAccordion>
  );
};
