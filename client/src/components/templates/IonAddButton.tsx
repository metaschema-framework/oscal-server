import React from 'react';
import { IonButton, IonIcon } from '@ionic/react';
import { add } from 'ionicons/icons';
import { IconButtonProps } from '@rjsf/utils';

const IonAddButton: React.FC<IconButtonProps> = (props) => {
  const { disabled, onClick } = props;

  const handleClick = (e: React.MouseEvent<HTMLIonButtonElement>) => {
    if (onClick) {
      // Convert the IonButton event to a regular button event
      onClick(e as unknown as React.MouseEvent<HTMLButtonElement>);
    }
  };

  return (
    <IonButton
      onClick={handleClick}
      disabled={disabled}
      color="primary"
      fill="clear"
      expand='full'
      size="default"
    >
      <IonIcon slot="start" icon={add} />
      Add Item
    </IonButton>
  );
};

export default IonAddButton;
