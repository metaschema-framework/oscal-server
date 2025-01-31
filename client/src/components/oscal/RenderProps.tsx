import React from 'react';
import { IonChip, IonLabel, IonBadge } from '@ionic/react';
import { Property } from '../../types';

interface RenderPropsProps {
  props: Property[];
}

export const RenderProps: React.FC<RenderPropsProps> = ({ props }) => {
  if (!props?.length) return null;
  
  return (
    <div className="props-container">
      {props.map((prop, index) => (
        <IonChip key={index} className="prop-chip" color="primary">
          <IonLabel>
            <strong>{prop.name}:</strong> {prop.value}
          </IonLabel>
          {prop.class && (
            <IonBadge color="light" className="prop-class">
              {prop.class}
            </IonBadge>
          )}
        </IonChip>
      ))}
    </div>
  );
};
