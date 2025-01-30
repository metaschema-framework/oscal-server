import {
  IonButton,
  IonButtons,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonIcon,
  IonItem,
  IonLabel,
  IonList,
  IonText,
} from "@ionic/react";
import { pencil, trash } from "ionicons/icons";
import React, { useState } from "react";
import { useOscal } from "../../context/OscalContext";
import { EntityType } from "../../types";
import OscalForm from "../OscalForm";

interface OscalListEditProps {
  type: EntityType;
  title: string;
  renderItemPreview: (item: any) => React.ReactNode;
  renderItemDetails: (item: any) => React.ReactNode;
}

const OscalListEdit: React.FC<OscalListEditProps> = ({
  type,
  title,
  renderItemPreview,
  renderItemDetails,
}) => {
  const { insert, all, destroy } = useOscal();
  const [items, setItems] = useState<Record<string, any>>({});

  React.useEffect(() => {
    const loadItems = async () => {
      const data = await all(type) || {};
      setItems(data);
    };
    loadItems();
  }, [all, type]);
  const [showForm, setShowForm] = useState(false);
  const [selectedItem, setSelectedItem] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);

  const handleDelete = async (uuid: string) => {
    await destroy(type, uuid);
    setSelectedItem(null);
  };

  const handleSelect = (uuid: string) => {
    setSelectedItem(uuid === selectedItem ? null : uuid);
    setIsEditing(false);
  };

  const handleEdit = () => {
    setIsEditing(true);
  };

  const renderListItem = (item: any) => (
    <IonItem 
      key={item.uuid} 
      button 
      onClick={() => handleSelect(item.uuid)}
      color={selectedItem === item.uuid ? "light" : undefined}
    >
      {renderItemPreview(item)}
      <IonButtons slot="end">
        <IonButton fill="clear" onClick={(e) => {
          e.stopPropagation();
          handleDelete(item.uuid);
        }}>
          <IonIcon icon={trash} />
        </IonButton>
      </IonButtons>
    </IonItem>
  );

  return (
    <div className="ion-padding">
      <IonCard>
        <IonCardHeader>
          <IonCardTitle>{title}</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          <IonList>
            {Object.values(items).length > 0 ? (
              Object.values(items).map(renderListItem)
            ) : (
              <IonItem>
                <IonLabel>
                  <IonText color="medium">No {title.toLowerCase()} defined yet.</IonText>
                </IonLabel>
              </IonItem>
            )}
          </IonList>
          <IonButton expand="block" onClick={() => {
            setShowForm(!showForm);
            setSelectedItem(null);
            setIsEditing(false);
          }}>
            {showForm ? "Hide Form" : `Add ${title.slice(0, -1)}`}
          </IonButton>
        </IonCardContent>
      </IonCard>

      {selectedItem && !isEditing && !showForm && (
        <IonCard>
          <IonCardHeader>
            <IonCardTitle>Details</IonCardTitle>
            <IonButton 
              fill="clear" 
              onClick={handleEdit}
              slot="end"
            >
              <IonIcon icon={pencil} slot="start" />
              Edit
            </IonButton>
          </IonCardHeader>
          <IonCardContent>
            {renderItemDetails(items[selectedItem])}
          </IonCardContent>
        </IonCard>
      )}

      {(showForm || isEditing) && (
        <IonCard>
          <IonCardHeader>
            <IonCardTitle>
              {isEditing ? `Edit ${title.slice(0, -1)}` : `Add New ${title.slice(0, -1)}`}
            </IonCardTitle>
          </IonCardHeader>
          <IonCardContent>
            <OscalForm
              type={type}
              initialData={isEditing ? items[selectedItem!] : undefined}
              onSubmit={(data) => {
                if (isEditing) {
                  insert(type, { ...data, uuid: selectedItem });
                  setIsEditing(false);
                } else {
                  insert(type, data);
                  setShowForm(false);
                }
              }}
            />
          </IonCardContent>
        </IonCard>
      )}
    </div>
  );
};

export default OscalListEdit;
